package com.bikininjas.corelib.command;

import com.bikininjas.corelib.objective.ChallengeDefinition;
import com.bikininjas.corelib.objective.ChallengeRegistry;
import com.bikininjas.corelib.objective.ObjectiveTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Brigadier command tree for challenge management.
 * <p>
 * Registered by {@link CommandRegister} on the NeoForge event bus.
 * All methods are static; no instance is required.
 */
public final class ChallengeCommand {

    private ChallengeCommand() {
    }

    /**
     * Register the {@code /challenge} command tree on the given dispatcher.
     *
     * @param dispatcher the command dispatcher to register on
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("challenge")
                .then(Commands.literal("list")
                        .executes(ChallengeCommand::handleList)
                )
                .then(Commands.literal("start")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(ChallengeCommand::suggestChallenges)
                                .executes(ChallengeCommand::handleStart)
                        )
                )
                .then(Commands.literal("status")
                        .executes(ChallengeCommand::handleStatus)
                )
                .then(Commands.literal("abort")
                        .requires(src -> src.getPlayer() != null)
                        .executes(ChallengeCommand::handleAbort)
                )
        );
    }

    // ──────────────────────────────────────────────
    //  Handlers
    // ──────────────────────────────────────────────

    private static int handleList(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var definitions = ChallengeRegistry.getAvailable();

        if (definitions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No challenges available.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Available Challenges:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (var def : definitions) {
            int objCount = def.objectives().size();
            String limit = def.timeLimitSeconds() > 0
                    ? String.format(" (%d:%02d)",
                    def.timeLimitSeconds() / 60, def.timeLimitSeconds() % 60)
                    : "";
            source.sendSuccess(() -> Component.literal(" \u2022 ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format(" - %d objective(s)%s", objCount, limit))
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        return definitions.size();
    }

    private static int handleStart(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can start challenges.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        ChallengeDefinition def = ChallengeRegistry.get(name);
        if (def == null) {
            source.sendFailure(Component.literal("Challenge '" + name + "' not found.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Check mod requirements.
        if (!ChallengeRegistry.areModsLoaded(def)) {
            source.sendFailure(Component.literal("Required mods not loaded for challenge '" + name + "'.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Check no active challenge.
        if (ObjectiveTracker.isTracking(player)) {
            source.sendFailure(Component.literal("You already have an active challenge. Use /challenge abort first.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ObjectiveTracker.startChallenge(player, def);

        source.sendSuccess(() -> Component.literal("✔ Challenge '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("' started!").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static int handleStatus(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can check status.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!ObjectiveTracker.isTracking(player)) {
            source.sendFailure(Component.literal("You have no active challenge.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String name = ObjectiveTracker.getActiveChallengeName(player);
        float progress = ObjectiveTracker.getProgress(player);
        int pct = Math.round(progress * 100.0f);
        long elapsed = ObjectiveTracker.getElapsedSeconds(player);
        long mins = elapsed / 60;
        long secs = elapsed % 60;

        source.sendSuccess(() -> Component.literal("Challenge: ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(name != null ? name : "?").withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Progress: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(pct + "%").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" | Time: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%d:%02d", mins, secs)).withStyle(ChatFormatting.YELLOW)), false);

        var objectives = ObjectiveTracker.getObjectives(player);
        for (var obj : objectives) {
            int cur = obj.progressValue(player);
            int tgt = obj.target();
            Component marker = obj.isComplete(player)
                    ? Component.literal("✔").withStyle(ChatFormatting.GREEN)
                    : Component.literal("\u2B1C").withStyle(ChatFormatting.GRAY);
            source.sendSuccess(() -> Component.literal(" ")
                    .append(marker)
                    .append(Component.literal(" " + obj.description()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format(" (%d/%d)", cur, tgt)).withStyle(ChatFormatting.GRAY)), false);
        }
        return objectives.size();
    }

    private static int handleAbort(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can abort challenges.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!ObjectiveTracker.isTracking(player)) {
            source.sendFailure(Component.literal("You have no active challenge to abort.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String name = ObjectiveTracker.getActiveChallengeName(player);
        ObjectiveTracker.stopChallenge(player);

        source.sendSuccess(() -> Component.literal("Challenge '")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(name != null ? name : "?").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("' aborted.").withStyle(ChatFormatting.YELLOW)), false);
        return 1;
    }

    // ──────────────────────────────────────────────
    //  Tab-completion suggestions
    // ──────────────────────────────────────────────

    private static CompletableFuture<Suggestions> suggestChallenges(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var definitions = ChallengeRegistry.getAvailable();
        for (var def : definitions) {
            if (def.name().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(def.name(), Component.literal(def.displayName()));
            }
        }
        return builder.buildFuture();
    }
}
