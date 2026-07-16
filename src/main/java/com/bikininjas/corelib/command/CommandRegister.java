package com.bikininjas.corelib.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers all core-lib commands on the NeoForge event bus.
 * <p>
 * This class is loaded by {@code CoreLib.initModules()} to trigger its
 * static initialiser, which subscribes to {@link RegisterCommandsEvent}
 * on the NeoForge event bus.
 */
public final class CommandRegister {

    private CommandRegister() {
    }

    static {
        NeoForge.EVENT_BUS.register(CommandHandler.class);
    }

    /**
     * Force-load this class (triggers {@code static} block).
     * Idempotent — safe to call multiple times.
     */
    public static void init() {
        // static block does the work
    }

    private static final class CommandHandler {

        private CommandHandler() {
        }

        @SubscribeEvent
        static void onRegisterCommands(RegisterCommandsEvent event) {
            ChallengeCommand.register(event.getDispatcher());
            StatsCommand.register(event.getDispatcher());
            KitCommand.register(event.getDispatcher());
        }
    }
}
