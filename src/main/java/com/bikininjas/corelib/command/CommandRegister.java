package com.bikininjas.corelib.command;

import com.bikininjas.corelib.kit.KitManager;
import com.bikininjas.corelib.objective.ChallengeDefinition;
import com.bikininjas.corelib.objective.ChallengeRegistry;
import com.bikininjas.corelib.objective.ObjectiveTracker;
import com.bikininjas.corelib.stats.PlayerStatsManager;
import com.bikininjas.corelib.time.TimeManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Registers core-lib commands ({@code /kit}, {@code /stats}, {@code /time}, {@code /challenge})
 * on the NeoForge event bus.
 * <p>
 * All methods are static. Registered via static initializer.
 */
public final class CommandRegister {

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private CommandRegister() {
    }

    /**
     * Force class loading (called from {@code CoreLib.initModules()}).
     * Triggers static initializer (event bus registration).
     */
    public static void init() {
        // static initializer already ran; explicit call for module loading
    }

    // -- Event handler -------------------------------------------------------

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
            var dispatcher = event.getDispatcher();
            registerKitCommand(dispatcher);
            registerStatsCommand(dispatcher);
            registerTimeCommand(dispatcher);
            registerChallengeCommand(dispatcher);
        }
    }

    // =========================================================================
    // /kit command
    // =========================================================================

    /**
     * Register the {@code /kit} command tree:
     * <ul>
     *   <li>{@code /kit list} — list available kits</li>
     *   <li>{@code /kit give <name>} — give a kit to the executing player</li>
     * </ul>
     */
    private static void registerKitCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        var kitNode = Commands.literal("kit")
                .requires(ctx -> ctx.hasPermission(0))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var names = KitManager.getAll();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Available kits: " + String.join(", ", names)),
                                    false);
                            return names.size();
                        }))
                .then(Commands.literal("give")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .requires(ctx -> ctx.hasPermission(2))
                                .suggests((ctx, builder) -> {
                                    for (var name : KitManager.getAll()) {
                                        builder.suggest(name);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    var name = StringArgumentType.getString(ctx, "name");
                                    var sender = ctx.getSource();
                                    if (sender.getPlayer() instanceof ServerPlayer player) {
                                        if (KitManager.give(player, name)) {
                                            sender.sendSuccess(
                                                    () -> Component.literal("Kit '" + name + "' given."),
                                                    false);
                                            return Command.SINGLE_SUCCESS;
                                        } else {
                                            sender.sendFailure(
                                                    Component.literal("Unknown kit: " + name));
                                            return 0;
                                        }
                                    }
                                    sender.sendFailure(Component.literal("Players only."));
                                    return 0;
                                })));
        dispatcher.register(kitNode);
    }

    // =========================================================================
    // /stats command
    // =========================================================================

    /**
     * Register the {@code /stats} command tree:
     * <ul>
     *   <li>{@code /stats} — show your own stats</li>
     *   <li>{@code /stats <player>} — show target player stats (permission 2)</li>
     *   <li>{@code /stats reset <player>} — reset target player stats (permission 2)</li>
     * </ul>
     */
    private static void registerStatsCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        var statsNode = Commands.literal("stats")
                .requires(ctx -> ctx.hasPermission(0))
                // /stats — own stats
                .executes(ctx -> {
                    var sender = ctx.getSource();
                    if (sender.getPlayer() instanceof ServerPlayer player) {
                        var deaths = PlayerStatsManager.getDeaths(player);
                        var kills = PlayerStatsManager.getKills(player);
                        var blocks = PlayerStatsManager.getBlocksBroken(player);
                        var crafts = PlayerStatsManager.getCrafts(player);
                        sender.sendSuccess(
                                () -> Component.literal("Your stats — Deaths: " + deaths
                                        + ", Kills: " + kills
                                        + ", Blocks Broken: " + blocks
                                        + ", Crafts: " + crafts),
                                false);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendFailure(Component.literal("Players only."));
                    return 0;
                })
                // /stats <player>
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var target = EntityArgument.getPlayer(ctx, "player");
                            var deaths = PlayerStatsManager.getDeaths(target);
                            var kills = PlayerStatsManager.getKills(target);
                            var blocks = PlayerStatsManager.getBlocksBroken(target);
                            var crafts = PlayerStatsManager.getCrafts(target);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(target.getName().getString()
                                            + "'s stats — Deaths: " + deaths
                                            + ", Kills: " + kills
                                            + ", Blocks Broken: " + blocks
                                            + ", Crafts: " + crafts),
                                    false);
                            return Command.SINGLE_SUCCESS;
                        })
                        // /stats reset <player>
                        .then(Commands.literal("reset")
                                .requires(ctx -> ctx.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            var target = EntityArgument.getPlayer(ctx, "player");
                                            PlayerStatsManager.resetStats(target);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Stats reset for "
                                                            + target.getName().getString()),
                                                    true);
                                            return Command.SINGLE_SUCCESS;
                                        }))));
        dispatcher.register(statsNode);
    }

    // =========================================================================
    // /time command
    // =========================================================================

    /**
     * Register the {@code /time} command tree:
     * <ul>
     *   <li>{@code /time day|night|noon|midnight} — set time on all levels (permission 2)</li>
     *   <li>{@code /time rate} — show current time rate</li>
     *   <li>{@code /time rate <0-100>} — set time speed (permission 2)</li>
     * </ul>
     */
    private static void registerTimeCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        var timeNode = Commands.literal("time")
                .requires(ctx -> ctx.hasPermission(0))
                // /time day — set to 1000
                .then(Commands.literal("day")
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            for (var level : server.getAllLevels()) {
                                TimeManager.setDay(level);
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set time to day on all dimensions."),
                                    true);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /time night — set to 13000
                .then(Commands.literal("night")
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            for (var level : server.getAllLevels()) {
                                TimeManager.setNight(level);
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set time to night on all dimensions."),
                                    true);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /time noon — set to 6000
                .then(Commands.literal("noon")
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            for (var level : server.getAllLevels()) {
                                TimeManager.setTime(level, 6000);
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set time to noon on all dimensions."),
                                    true);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /time midnight — set to 18000
                .then(Commands.literal("midnight")
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            for (var level : server.getAllLevels()) {
                                TimeManager.setTime(level, 18000);
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set time to midnight on all dimensions."),
                                    true);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /time rate
                .then(Commands.literal("rate")
                        // /time rate — show current rate
                        .executes(ctx -> {
                            var level = ctx.getSource().getLevel();
                            var rate = TimeManager.getTimeRate(level);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Current time rate: " + rate + "x"),
                                    false);
                            return Command.SINGLE_SUCCESS;
                        })
                        // /time rate <0-100> — set rate on all levels
                        .then(Commands.argument("rate", IntegerArgumentType.integer(0, 100))
                                .requires(ctx -> ctx.hasPermission(2))
                                .executes(ctx -> {
                                    var rate = IntegerArgumentType.getInteger(ctx, "rate");
                                    var server = ctx.getSource().getServer();
                                    for (var level : server.getAllLevels()) {
                                        TimeManager.setTimeRate(level, (float) rate);
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Set time rate to " + rate + "x on all dimensions."),
                                    true);
                                    return Command.SINGLE_SUCCESS;
                                })));
        dispatcher.register(timeNode);
    }

    // =========================================================================
    // /challenge command
    // =========================================================================

    /**
     * Register the {@code /challenge} command tree:
     * <ul>
     *   <li>{@code /challenge list} — show available challenges</li>
     *   <li>{@code /challenge start <name>} — start a challenge (permission 2, suggests)</li>
     *   <li>{@code /challenge stop} — stop the active challenge (permission 2)</li>
     *   <li>{@code /challenge progress} — show current challenge progress</li>
     * </ul>
     */
    private static void registerChallengeCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        var challengeNode = Commands.literal("challenge")
                .requires(ctx -> ctx.hasPermission(0))
                // /challenge list
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var available = ChallengeRegistry.getAvailable();
                            if (available.isEmpty()) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("No challenges available."),
                                        false);
                                return 0;
                            }
                            var names = available.stream()
                                    .map(ChallengeDefinition::displayName)
                                    .collect(Collectors.joining(", "));
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Available challenges: " + names),
                                    false);
                            return available.size();
                        }))
                // /challenge start <name>
                .then(Commands.literal("start")
                        .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (var def : ChallengeRegistry.getAvailable()) {
                                        builder.suggest(def.name());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    var name = StringArgumentType.getString(ctx, "name");
                                    var sender = ctx.getSource();
                                    if (sender.getPlayer() instanceof ServerPlayer player) {
                                        var definition = ChallengeRegistry.get(name);
                                        if (definition == null) {
                                            sender.sendFailure(
                                                    Component.literal("Unknown challenge: " + name));
                                            return 0;
                                        }
                                        if (ObjectiveTracker.isTracking(player)) {
                                            sender.sendFailure(
                                                    Component.literal("You already have an active challenge. Use /challenge stop first."));
                                            return 0;
                                        }
                                        var challenge = definition.toChallenge();
                                        ObjectiveTracker.startChallenge(player, challenge);
                                        sender.sendSuccess(
                                                () -> Component.literal("Challenge '" + definition.displayName() + "' started!"),
                                                true);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    sender.sendFailure(Component.literal("Players only."));
                                    return 0;
                                })))
                // /challenge stop
                .then(Commands.literal("stop")
                        .requires(ctx -> ctx.hasPermission(2))
                        .executes(ctx -> {
                            var sender = ctx.getSource();
                            if (sender.getPlayer() instanceof ServerPlayer player) {
                                if (!ObjectiveTracker.isTracking(player)) {
                                    sender.sendFailure(
                                            Component.literal("You have no active challenge."));
                                    return 0;
                                }
                                var name = ObjectiveTracker.getActiveChallengeName(player);
                                ObjectiveTracker.stopChallenge(player);
                                sender.sendSuccess(
                                        () -> Component.literal("Challenge '" + name + "' stopped."),
                                        true);
                                return Command.SINGLE_SUCCESS;
                            }
                            sender.sendFailure(Component.literal("Players only."));
                            return 0;
                        }))
                // /challenge progress
                .then(Commands.literal("progress")
                        .executes(ctx -> {
                            var sender = ctx.getSource();
                            if (sender.getPlayer() instanceof ServerPlayer player) {
                                if (!ObjectiveTracker.isTracking(player)) {
                                    sender.sendFailure(
                                            Component.literal("You have no active challenge."));
                                    return 0;
                                }
                                var name = ObjectiveTracker.getActiveChallengeName(player);
                                var progress = ObjectiveTracker.getProgress(player);
                                var elapsed = ObjectiveTracker.getElapsedSeconds(player);
                                var pct = Math.round(progress * 100.0f);
                                sender.sendSuccess(
                                        () -> Component.literal("Challenge '" + name + "' — "
                                                + pct + "% complete, "
                                                + elapsed + "s elapsed"),
                                        false);
                                return Command.SINGLE_SUCCESS;
                            }
                            sender.sendFailure(Component.literal("Players only."));
                            return 0;
                        }));
        dispatcher.register(challengeNode);
    }
}
