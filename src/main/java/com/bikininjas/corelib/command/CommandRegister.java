package com.bikininjas.corelib.command;

import com.bikininjas.corelib.kit.KitManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Registers core-lib commands ({@code /kit}) on the NeoForge event bus.
 * <p>
 * All methods are static. Registered via static initializer.
 */
public final class CommandRegister {

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private CommandRegister() {
    }

    // -- Event handler -------------------------------------------------------

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
            registerKitCommand(event.getDispatcher());
        }
    }

    // -- /kit command --------------------------------------------------------

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
}
