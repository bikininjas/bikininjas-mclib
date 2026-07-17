package com.bikininjas.corelib.client;

import com.bikininjas.corelib.CoreLib;
import com.bikininjas.corelib.config.client.BikiniConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Keybind to open the Bikini Config screen (default: B key).
 */
public final class ConfigKeybinds {

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key." + CoreLib.MODID + ".config",
            InputConstants.KEY_B,
            "key.categories." + CoreLib.MODID
    );

    private ConfigKeybinds() {
    }

    /**
     * Register key mapping on the mod bus (called from CoreLib constructor).
     */
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    static {
        NeoForge.EVENT_BUS.register(ClientHandler.class);
    }

    public static void init() {
    }

    private static final class ClientHandler {
        private ClientHandler() {
        }

        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_CONFIG.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new BikiniConfigScreen());
                }
            }
        }
    }
}
