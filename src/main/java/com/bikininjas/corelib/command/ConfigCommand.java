package com.bikininjas.corelib.command;

import com.bikininjas.corelib.config.BikiniConfigRegistry;
import com.bikininjas.corelib.config.client.BikiniConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code /bikininjas} command (alias {@code /bn}).
 * Opens the Bikini Config GUI on the client, or shows registered mods on the server.
 */
public final class ConfigCommand {

    private ConfigCommand() {}

    public static void init() {
        NeoForge.EVENT_BUS.register(Handler.class);
    }

    private static final class Handler {
        @SubscribeEvent
        static void onRegisterCommands(RegisterCommandsEvent event) {
            register(event.getDispatcher());
        }
    }

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        var node = Commands.literal("bikininjas")
                .then(Commands.literal("config")
                        .executes(ctx -> executeConfig(ctx.getSource())))
                .executes(ctx -> executeConfig(ctx.getSource()));

        dispatcher.register(node);
        dispatcher.register(Commands.literal("bn").redirect(node.build()));
    }

    private static int executeConfig(@NotNull CommandSourceStack source) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            var mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new BikiniConfigScreen()));
            source.sendSuccess(() -> Component.literal("Opening Bikini Config..."), false);
        } else {
            var mods = BikiniConfigRegistry.getRegisteredMods();
            if (mods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No Bikini mods registered."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Registered mods: " + String.join(", ", mods)), false);
            }
        }
        return 1;
    }
}
