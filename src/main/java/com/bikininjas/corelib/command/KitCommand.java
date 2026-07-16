package com.bikininjas.corelib.command;

import com.bikininjas.corelib.kit.KitManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Brigadier command tree for kit management.
 * <p>
 * Registered by {@link CommandRegister} on the NeoForge event bus.
 * All methods are static; no instance is required.
 */
public final class KitCommand {

    private KitCommand() {}

    /**
     * Register the {@code /kit} command tree on the given dispatcher.
     *
     * @param dispatcher the command dispatcher to register on
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
                .then(Commands.literal("list")
                        .executes(KitCommand::handleList)
                )
                .then(Commands.literal("give")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(KitCommand::suggestKits)
                                .executes(ctx -> handleGive(ctx, ctx.getSource().getPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> handleGive(ctx, EntityArgument.getPlayer(ctx, "player")))
                                )
                        )
                )
        );
    }

    // ──────────────────────────────────────────────
    //  Handlers
    // ──────────────────────────────────────────────

    private static int handleList(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var names = KitManager.getAll();

        if (names.isEmpty()) {
            source.sendFailure(Component.literal("§7No kits available."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§lAvailable Kits:"), false);
        for (String name : names) {
            source.sendSuccess(() -> Component.literal(" §e• §f" + name), false);
        }
        return names.size();
    }

    private static int handleGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        var source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        if (KitManager.give(target, name)) {
            String msg = "§a✔ Kit '§f" + name + "§a' given to §f" + target.getScoreboardName();
            source.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cKit '" + name + "' not found."));
            return 0;
        }
    }

    // ──────────────────────────────────────────────
    //  Tab-completion suggestions
    // ──────────────────────────────────────────────

    private static CompletableFuture<Suggestions> suggestKits(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String name : KitManager.getAll()) {
            if (name.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}
