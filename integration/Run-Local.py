import argparse
import pathlib
import subprocess
import threading
import time


def run_server(lab: pathlib.Path, output: pathlib.Path, soak_seconds: int,
               race: str | None, port: str, mode: str) -> None:
    debug_options = [f"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:{port}"] if race else []
    with output.open("w", encoding="utf-8") as log:
        server = subprocess.Popen(
            [
                "java", "-Xms512M", "-Xmx1600M", "-Dterminal.jline=false",
                "-Dterminal.ansi=false", "-Dfile.encoding=UTF-8", *debug_options, "-jar", "paper.jar", "nogui",
            ],
            cwd=lab, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
        )

        def command(value: str) -> None:
            server.stdin.write(value + "\n")
            server.stdin.flush()

        deadline = threading.Timer(soak_seconds + 360, server.kill)
        deadline.start()
        result = None
        gate = None
        threshold = False
        try:
            for line in server.stdout:
                log.write(line)
                log.flush()
                if "Not in threshold." in line:
                    threshold = True
                if "Done (" in line and "For help" in line:
                    time.sleep(soak_seconds)
                    if race:
                        gate_source = pathlib.Path(__file__).with_name("TimerRaceGate.java")
                        gate_line = "160" if race == "buggy" else "137"
                        gate = subprocess.Popen(
                            ["java", "--add-modules", "jdk.jdi", str(gate_source), port, gate_line],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                        )
                        ready = gate.stdout.readline().strip()
                        if ready != "READY":
                            raise RuntimeError(f"Race debugger failed to initialize. {ready}")
                        command(f"pshometest race-{race}")
                    else:
                        command(f"pshometest {mode}")
                if mode == "restart-arm" and "[PsHomeScenarios] restart armed pending=1" in line:
                    result = line.strip()
                    command("stop")
                    break
                if "[PsHomeScenarios] suite complete" in line:
                    result = line.strip()
                    command("stop")
                    break
                if ("[PsHomeScenarios] suite aborted" in line
                        or "Requires isolated localhost qa_world" in line
                        or "Requires default PS EMERALD_ORE" in line):
                    result = line.strip()
                    command("stop")
                    break
            else:
                raise RuntimeError(f"No suite result. See {output}")
            for line in server.stdout:
                log.write(line)
            if server.wait(timeout=30) != 0:
                raise RuntimeError(f"Server shutdown failed. See {output}")
            expected_result = "pending=1" if mode == "restart-arm" else "fail=0"
            if result is None or not result.endswith(expected_result):
                raise RuntimeError(f"{result}. See {output}")
            if gate is not None:
                gate_output = gate.communicate(timeout=20)[0]
                log.write(gate_output)
                if gate.returncode != 0 or "RELEASED two workers" not in gate_output:
                    raise RuntimeError(f"Race debugger failed. See {output}")
                if threshold != (race == "buggy"):
                    raise RuntimeError(f"Unexpected ProtectionStones movement decision. See {output}")
        finally:
            deadline.cancel()
            if gate is not None and gate.poll() is None:
                gate.kill()
                gate.communicate()
            if server.poll() is None:
                try:
                    command("stop")
                except OSError:
                    pass
                try:
                    server.communicate(timeout=30)
                except subprocess.TimeoutExpired:
                    server.kill()
                    server.communicate()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("lab", type=pathlib.Path)
    parser.add_argument("--soak-seconds", type=int, default=0)
    parser.add_argument("--label", default="suite")
    parser.add_argument("--race", choices=("buggy", "fixed"))
    parser.add_argument("--debug-port", type=int, default=25588)
    parser.add_argument("--mode", choices=("standard", "restart-arm", "restart-check"), default="standard")
    args = parser.parse_args()
    lab = args.lab.resolve()
    settings = set((lab / "server.properties").read_text(encoding="utf-8").splitlines())
    if not {"server-ip=127.0.0.1", "level-name=qa_world"} <= settings:
        parser.error("Use a disposable localhost server with level-name=qa_world.")
    if not (lab / "plugins" / "PsHomeScenarios.jar").is_file():
        parser.error("Install the fixture on the disposable server first.")
    if not args.label.replace("-", "").isalnum() or args.soak_seconds < 0:
        parser.error("Use a simple label and a nonnegative soak duration.")
    if not 1024 <= args.debug_port <= 65535 or (args.race and args.mode != "standard"):
        parser.error("Use a valid local debug port and a single test mode.")
    output = lab / f"{args.label}.log"
    run_server(lab, output, args.soak_seconds, args.race, str(args.debug_port), args.mode)
    print(output)


if __name__ == "__main__":
    main()
