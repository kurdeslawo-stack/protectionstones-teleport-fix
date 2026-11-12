import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.EventRequest;
import java.util.ArrayList;
import java.util.List;

class TimerRaceGate {
    private static final String ARG_TP = "dev.espi.protectionstones.commands.ArgTp";

    public static void main(String[] args) throws Exception {
        int line = Integer.parseInt(args[1]);
        var connector = Bootstrap.virtualMachineManager().attachingConnectors().stream()
                .filter(candidate -> candidate.name().equals("com.sun.jdi.SocketAttach"))
                .findFirst().orElseThrow();
        var parameters = connector.defaultArguments();
        parameters.get("hostname").setValue("127.0.0.1");
        parameters.get("port").setValue(args[0]);
        parameters.get("timeout").setValue("10000");
        VirtualMachine vm = connector.attach(parameters);
        ThreadReference main = null;
        try {
            var prepare = vm.eventRequestManager().createClassPrepareRequest();
            prepare.addClassFilter(ARG_TP);
            prepare.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            prepare.enable();
            for (ReferenceType type : vm.classesByName(ARG_TP)) {
                if (type.isPrepared()) {
                    install(vm, type, line);
                    prepare.disable();
                }
            }
            System.out.println("READY");
            List<EventSet> held = new ArrayList<>();
            List<ThreadReference> workers = new ArrayList<>();
            while (held.size() < 2) {
                EventSet set = next(vm);
                boolean hold = false;
                for (var event : set) {
                    if (event instanceof ClassPrepareEvent loaded) {
                        install(vm, loaded.referenceType(), line);
                        prepare.disable();
                    } else if (event instanceof BreakpointEvent reached
                            && !reached.thread().name().equals("Server thread")) {
                        hold = true;
                        reached.request().disable();
                        if (held.isEmpty()) {
                            reached.request().enable();
                        }
                        held.add(set);
                        workers.add(reached.thread());
                    }
                }
                if (!hold) {
                    set.resume();
                }
            }
            main = vm.allThreads().stream().filter(thread -> thread.name().equals("Server thread"))
                    .findFirst().orElseThrow();
            main.suspend();
            for (int i = 0; i < held.size(); i++) {
                var exit = vm.eventRequestManager().createMethodExitRequest();
                exit.addThreadFilter(workers.get(i));
                exit.addClassFilter(ARG_TP);
                exit.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                exit.enable();
                held.get(i).resume();
                while (true) {
                    var set = next(vm);
                    boolean exited = set.stream().anyMatch(event -> event instanceof MethodExitEvent completed
                            && completed.method().name().equals("teleportPlayer"));
                    if (exited) {
                        exit.disable();
                    }
                    set.resume();
                    if (exited) {
                        break;
                    }
                }
            }
            System.out.println("RELEASED two workers in order before the next server tick");
        } finally {
            if (main != null) {
                main.resume();
            }
            vm.dispose();
        }
    }

    private static void install(VirtualMachine vm, ReferenceType type, int line) throws Exception {
        var location = type.locationsOfLine(line).stream()
                .filter(candidate -> candidate.method().name().equals("teleportPlayer"))
                .findFirst().orElseThrow();
        var gate = vm.eventRequestManager().createBreakpointRequest(location);
        gate.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        gate.enable();
    }

    private static EventSet next(VirtualMachine vm) throws Exception {
        var set = vm.eventQueue().remove(15000);
        if (set == null) {
            throw new IllegalStateException("Expected timer event did not arrive");
        }
        return set;
    }
}
