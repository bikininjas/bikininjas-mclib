package com.bikininjas.corelib.time;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for controlling the day/night cycle on the server side.
 * <p>
 * All methods are static. No singleton.
 */
public final class TimeManager {

    private static final Map<ServerLevel, TimeState> timeStates = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private TimeManager() {
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     * Triggers static initializer (event bus registration).
     */
    public static void init() {
    }

    // -- Public API ----------------------------------------------------------

    /**
     * Set the absolute time of day (0–23999).
     */
    public static void setTime(@NotNull ServerLevel level, long timeOfDay) {
        Objects.requireNonNull(level, "level must not be null");
        level.setDayTime(timeOfDay);
    }

    /**
     * Set time to midday (1000).
     */
    public static void setDay(@NotNull ServerLevel level) {
        setTime(level, 1000);
    }

    /**
     * Set time to night time (13000).
     */
    public static void setNight(@NotNull ServerLevel level) {
        setTime(level, 13000);
    }

    /**
     * Add ticks to the current time.
     */
    public static void addTime(@NotNull ServerLevel level, long ticks) {
        Objects.requireNonNull(level, "level must not be null");
        level.setDayTime(level.getDayTime() + ticks);
    }

    /**
     * Set the time rate multiplier.
     *
     * @param rate 0 = frozen, 1 = normal, 72 = 72× speed, etc.
     */
    public static void setTimeRate(@NotNull ServerLevel level, float rate) {
        Objects.requireNonNull(level, "level must not be null");
        timeStates.computeIfAbsent(level, TimeState::new).rate = rate;
    }

    /**
     * Toggle time freeze for the given level.
     */
    public static void toggleTimeFreeze(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        var state = timeStates.computeIfAbsent(level, TimeState::new);
        state.frozen = !state.frozen;
    }

    /**
     * Check if time is frozen for the given level.
     */
    public static boolean isTimeFrozen(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        var state = timeStates.get(level);
        return state != null && state.frozen;
    }

    /**
     * Get the time rate multiplier for the given level.
     *
     * @return the rate (1.0 = normal, 0 = frozen, 72 = 72× speed, etc.)
     */
    public static float getTimeRate(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        var state = timeStates.get(level);
        return state != null ? state.rate : 1.0f;
    }

    /**
     * Get the current day time of the level.
     */
    public static long getDayTime(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        return level.getDayTime();
    }

    /**
     * Pure math helper: compute extra ticks for a given rate and tick delta.
     * Testable without a Minecraft runtime.
     *
     * @param rate       the time rate multiplier
     * @param tickDelta  the base tick increment (usually 1.0f)
     * @return extra ticks to add beyond the normal 1 per tick
     */
    public static long computeExtraTicks(float rate, float tickDelta) {
        if (rate <= 1.0f) {
            return 0;
        }
        return (long) ((rate - 1.0f) * tickDelta);
    }

    // -- Internal state ------------------------------------------------------

    private static final class TimeState {
        final ServerLevel level;
        float rate = 1.0f;
        boolean frozen = false;

        TimeState(ServerLevel level) {
            this.level = level;
        }
    }

    // -- Event handler -------------------------------------------------------

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onServerTick(@NotNull ServerTickEvent.Post event) {
            for (var entry : timeStates.entrySet()) {
                var level = entry.getKey();
                var state = entry.getValue();

                if (state.frozen) {
                    // Keep time frozen by reverting the auto-increment
                    level.setDayTime(level.getDayTime() - 1);
                } else if (state.rate > 1.0f) {
                    var extra = computeExtraTicks(state.rate, 1.0f);
                    if (extra > 0) {
                        level.setDayTime(level.getDayTime() + extra);
                    }
                }
            }
        }
    }
}
