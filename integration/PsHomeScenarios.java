package audit.protectionstones;

import dev.espi.protectionstones.BlockHandler;
import dev.espi.protectionstones.PSPlayer;
import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.api.FppBot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class PsHomeScenarios extends JavaPlugin implements Listener {
    private static final String BOT_NAME = "PsAuditBot";
    private static final int OBSERVATION_TICKS = 105;

    private FppBot bot;
    private Location origin;
    private Location home;
    private String homeId;
    private Location alternateHome;
    private String alternateHomeId;
    private int oldHomeEvents;
    private int homeEvents;
    private int homeSuccesses;
    private int commandTick;
    private int homeEventTick;
    private boolean vetoHome;
    private double maxY;
    private boolean running;
    private int passed;
    private int failed;
    private boolean restoreDebug;
    private boolean originalDebug;
    private PSProtectBlock options;
    private PermissionAttachment permissions;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender != Bukkit.getConsoleSender() || running) {
            return true;
        }
        String mode = args.length == 0 ? "standard" : args[0];
        if (args.length > 1 || !List.of("standard", "race-buggy", "race-fixed", "restart-arm", "restart-check").contains(mode)) {
            return false;
        }
        var world = Bukkit.getWorlds().getFirst();
        if (!Bukkit.getIp().equals("127.0.0.1") || !world.getName().equals("qa_world")
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            sender.sendMessage("Requires isolated localhost qa_world without connected players.");
            return true;
        }
        options = ProtectionStones.getBlockOptions("EMERALD_ORE");
        if (options == null || options.tpWaitingSeconds != 3 || !options.noMovingWhenTeleportWaiting
                || !ProtectionStones.getInstance().getConfigOptions().base_command.equals("ps")) {
            sender.sendMessage("Requires default PS EMERALD_ORE with 3-second movement guard and /ps.");
            return true;
        }
        running = true;
        passed = failed = 0;
        boolean race = mode.startsWith("race-");
        alternateHome = null;
        List<Scenario> plan = race ? raceCase(mode) : mode.equals("restart-check")
                ? List.of(new Scenario("after-restart", 1, 1, () -> { })) : cases();
        if (race) {
            originalDebug = ProtectionStones.getInstance().isDebug();
            restoreDebug = true;
            ProtectionStones.getInstance().setDebug(true);
        }
        try {
            require(pendingTeleportTasks() == 0, "teleport task already queued before suite");
            world.setGameRule(org.bukkit.GameRules.SPAWN_MOBS, false);
            origin = new Location(world, 256.5, 80, 256.5);
            var marker = origin.clone().add(96.5, 0, 0.5);
            for (Location location : List.of(origin, origin.clone().add(1.25, 0, 0), marker)) {
                location.clone().subtract(0, 1, 0).getBlock().setType(Material.STONE);
            }
            spawn(() -> {
                PSRegion primary = ensureHome(marker, !mode.equals("restart-check"));
                home = primary.getHome();
                homeId = primary.getId();
                require(home.distance(origin) > 80, "unexpected home location");
                if (race) {
                    PSRegion alternate = ensureHome(marker.clone().add(150, 0, 0), true);
                    alternateHome = alternate.getHome();
                    alternateHomeId = alternate.getId();
                }
                if (mode.equals("standard") && PSPlayer.fromPlayer(player()).getHomes(world).size() == 1) {
                    plan.addFirst(new Scenario("bare-home", 1, 1, () -> { }));
                }
                later(30, () -> {
                    if (mode.equals("restart-arm")) {
                        homeCommand();
                        later(20, () -> {
                            require(pendingTeleportTasks() == 1, "expected one pending teleport before shutdown");
                            getLogger().info("restart armed pending=1");
                        });
                    } else {
                        runCases(plan, 0);
                    }
                });
            });
        } catch (RuntimeException exception) {
            abort(exception);
        }
        return true;
    }

    private PSRegion ensureHome(Location marker, boolean allowCreate) {
        var existing = PSPlayer.fromPlayer(player()).getHomes(marker.getWorld()).stream()
                .filter(region -> region.getHome().distanceSquared(marker) < 4).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        require(allowCreate, "synthetic region was not restored from disk");
        marker.getBlock().setType(Material.EMERALD_ORE);
        require(BlockHandler.createActualRegion(player(), marker, options), "cannot create synthetic PS region");
        return ensureHome(marker, false);
    }

    private List<Scenario> raceCase(String variant) {
        int expected = variant.equals("race-buggy") ? 0 : 1;
        return List.of(new Scenario(variant, expected, expected, () -> later(20, () -> {
            require(player().teleport(origin.clone().add(1.25, 0, 0)), "cannot set position for second request");
            commandTick = Bukkit.getCurrentTick();
            require(player().performCommand("ps home " + alternateHomeId), "second PS home command unavailable");
        })));
    }

    private List<Scenario> cases() {
        List<Scenario> result = new ArrayList<>();
        result.add(new Scenario("stationary", 1, 1, () -> { }));
        result.add(new Scenario("camera-only", 1, 1, () -> later(15, () -> player().setRotation(170, 35))));
        result.add(new Scenario("delta-x-0.001", 1, 1, () -> move(0.001)));
        result.add(new Scenario("delta-x-1.0", 1, 1, () -> move(1.0)));
        result.add(new Scenario("delta-x-1.25", 0, 0, () -> move(1.25)));
        result.add(new Scenario("jump", 0, 0, () -> later(16, () -> {
            player().setGravity(true);
            player().setVelocity(new Vector(0, 0.42, 0));
        })));
        result.add(new Scenario("after-jump", 1, 1, () -> { }));
        result.add(new Scenario("replace-timer", 1, 1, () -> later(20, this::homeCommand)));
        result.add(new Scenario("replace-then-move", 0, 0, () -> {
            later(20, this::homeCommand);
            later(40, () -> require(player().teleport(origin.clone().add(1.25, 0, 0)), "test movement cancelled"));
        }));
        result.add(new Scenario("explicit-veto", 1, 0, () -> vetoHome = true));
        result.add(new Scenario("logout", 0, 0, () -> later(20, () -> {
            UUID uuid = player().getUniqueId();
            despawn();
            later(20, () -> require(Bukkit.getPlayer(uuid) == null, "bot still visible to Bukkit after logout"));
        })));
        result.add(new Scenario("after-reconnect", 1, 1, () -> { }));
        for (int i = 1; i <= 6; i++) {
            result.add(new Scenario("serial-" + i, 1, 1, () -> { }));
        }
        for (int i = 1; i <= 4; i++) {
            result.add(new Scenario("burst-" + i, 1, 1, () -> {
                for (int command = 0; command < 23; command++) {
                    homeCommand();
                }
            }));
        }
        result.add(new Scenario("after-bursts", 1, 1, () -> { }));
        result.add(new Scenario("bypass-wait", 1, 1, () -> { }));
        result.add(new Scenario("zero-wait", 1, 1, () -> { }));
        result.add(new Scenario("unguarded-delay", 1, 1, () -> move(1.25)));
        return result;
    }

    private void runCases(List<Scenario> scenarios, int index) {
        if (index == scenarios.size()) {
            despawn();
            restoreDebug();
            resetOptions();
            running = false;
            getLogger().info("suite complete pass=" + passed + " fail=" + failed);
            return;
        }
        if (bot == null) {
            spawn(() -> runCases(scenarios, index));
            return;
        }
        Player current = player();
        current.setGravity(false);
        require(current.teleport(origin), "failed to reset synthetic player");
        current.setVelocity(new Vector());
        Scenario scenario = scenarios.get(index);
        later(10, () -> {
            homeEvents = homeSuccesses = oldHomeEvents = 0;
            homeEventTick = -1;
            vetoHome = false;
            maxY = 0;
            options.tpWaitingSeconds = scenario.name.equals("zero-wait") ? 0 : 3;
            options.noMovingWhenTeleportWaiting = !scenario.name.equals("unguarded-delay");
            permissions.setPermission("protectionstones.tp.bypasswait", scenario.name.equals("bypass-wait"));
            if (Bukkit.getPluginManager().isPluginEnabled("PsHomeTrace")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pshometrace arm " + BOT_NAME);
            }
            if (scenario.name.equals("bare-home")) {
                commandTick = Bukkit.getCurrentTick();
                require(player().performCommand("ps home"), "bare PS home command unavailable");
            } else {
                homeCommand();
            }
            scenario.action.run();
            if (scenario.name.equals("jump")) {
                for (int tick = 16; tick <= 32; tick++) {
                    later(tick, () -> maxY = Math.max(maxY, player().getY() - origin.getY()));
                }
            }
            later(OBSERVATION_TICKS, () -> {
                if (Bukkit.getPluginManager().isPluginEnabled("PsHomeTrace")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pshometrace off");
                }
                Location target = alternateHome == null ? home : alternateHome;
                boolean arrived = bot != null && player().getWorld().equals(target.getWorld())
                        && player().getLocation().distanceSquared(target) < 0.01;
                int pending = pendingTeleportTasks();
                int expectedPending = scenario.name.equals("race-buggy") ? 1 : 0;
                int ticks = homeEventTick < 0 ? -1 : homeEventTick - commandTick;
                boolean instant = scenario.name.equals("zero-wait") || scenario.name.equals("bypass-wait");
                boolean timing = homeEventTick < 0 || (instant ? ticks >= 0 && ticks < 60 : ticks >= 60);
                boolean pass = homeEvents == scenario.expectedEvents && homeSuccesses == scenario.expectedSuccesses
                        && arrived == (scenario.expectedSuccesses > 0)
                        && pending == expectedPending && oldHomeEvents == 0 && timing
                        && (!scenario.name.equals("jump") || maxY > 1.0);
                if (pass) {
                    passed++;
                } else {
                    failed++;
                }
                getLogger().info((pass ? "PASS " : "FAIL ") + scenario.name
                        + " attempts=" + homeEvents + " successfulEvents=" + homeSuccesses
                        + " oldHomeEvents=" + oldHomeEvents + " pending=" + pending + " elapsedTicks=" + ticks
                        + (scenario.name.equals("jump") ? " jumpPeak=" + maxY : ""));
                runCases(scenarios, index + 1);
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void veto(PlayerTeleportEvent event) {
        if (isHome(event) && vetoHome) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void count(PlayerTeleportEvent event) {
        if (isHome(event)) {
            if (alternateHome != null && event.getTo().distanceSquared(home) < 0.01) {
                oldHomeEvents++;
                return;
            }
            homeEvents++;
            homeEventTick = Bukkit.getCurrentTick();
            if (!event.isCancelled()) {
                homeSuccesses++;
            }
        }
    }

    private boolean isHome(PlayerTeleportEvent event) {
        return running && bot != null && event.getPlayer().getUniqueId().equals(bot.getUuid()) && home != null
                && event.getTo().getWorld().equals(home.getWorld()) && (event.getTo().distanceSquared(home) < 0.01
                || (alternateHome != null && event.getTo().distanceSquared(alternateHome) < 0.01));
    }

    private void spawn(Runnable ready) {
        bot = FakePlayerPlugin.getInstance().getFppApi().spawnBot(origin, null, BOT_NAME).orElseThrow();
        awaitSpawn(ready, 60);
    }

    private void awaitSpawn(Runnable ready, int attempts) {
        Player current = player();
        if (current == null || Bukkit.getPlayer(current.getUniqueId()) == null) {
            require(attempts > 0, "bot did not become a Bukkit-online player");
            later(2, () -> awaitSpawn(ready, attempts - 1));
            return;
        }
        permissions = current.addAttachment(this);
        permissions.setPermission("protectionstones.home", true);
        permissions.setPermission("protectionstones.tp.bypasswait", false);
        current.setGravity(false);
        ready.run();
    }

    private void despawn() {
        if (bot != null) {
            FakePlayerPlugin.getInstance().getFppApi().despawnBot(bot);
            bot = null;
        }
    }

    private Player player() {
        return bot.getEntity();
    }

    private void move(double delta) {
        later(20, () -> require(player().teleport(origin.clone().add(delta, 0, 0)), "test movement cancelled"));
    }

    private void homeCommand() {
        commandTick = Bukkit.getCurrentTick();
        require(player().performCommand("ps home " + homeId), "PS home command unavailable");
    }

    private int pendingTeleportTasks() {
        return (int) Bukkit.getScheduler().getPendingTasks().stream()
                .filter(task -> task.getOwner() == ProtectionStones.getInstance())
                .filter(this::isTeleportTask)
                .count();
    }

    private boolean isTeleportTask(BukkitTask task) {
        try {
            // Paper 26.3 fixture only: inspect the scheduler, including tasks no longer referenced by ArgTp.
            Object action = task.getClass().getField("rTask").get(task);
            return action != null && action.getClass().getName()
                    .startsWith("dev.espi.protectionstones.commands.ArgTp$");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("cannot identify pending PS tasks on this Paper build", exception);
        }
    }

    private void later(long delay, Runnable action) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                abort(exception);
            }
        }, delay);
    }

    private void abort(RuntimeException exception) {
        getLogger().log(java.util.logging.Level.SEVERE, "suite aborted", exception);
        Bukkit.getScheduler().cancelTasks(this);
        despawn();
        restoreDebug();
        resetOptions();
        running = false;
    }

    @Override
    public void onDisable() {
        restoreDebug();
        resetOptions();
    }

    private void resetOptions() {
        if (options != null) {
            options.tpWaitingSeconds = 3;
            options.noMovingWhenTeleportWaiting = true;
        }
    }

    private void restoreDebug() {
        if (restoreDebug) {
            ProtectionStones.getInstance().setDebug(originalDebug);
            restoreDebug = false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record Scenario(String name, int expectedEvents, int expectedSuccesses, Runnable action) { }
}
