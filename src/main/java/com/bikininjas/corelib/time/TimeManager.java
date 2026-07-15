package com.bikininjas.corelib.time;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side time manipulation utility for NeoForge mods.
 * <p>
 * Provides a small, stateless API to control the day/night cycle of a
 * {@link ServerLevel}: jump to midday/midnight, scale the game time rate
 * (including freezing it), and query the current state.
 * <p>
 * All state is kept in a static {@link Map} keyed by the level's
 * {@link ResourceKey} so that multiple dimensions are tracked independently.
 * The actual per-tick time advancement is driven by a
 * {@link ServerTickEvent.Post} handler registered on the NeoForge event bus.
 * <p>
 * This class is server-only by design — it never touches client-side code.
 */
public final class TimeManager {

    private TimeManager() {}

    // ──────────────────────────────────────────────
    //  State storage
    // ──────────────────────────────────────────────

    /**
     * Per-dimension time state: whether time is frozen and the current rate.
     *
     * @param frozen {@code true} when the dimension's time is held constant
     * @param rate   game time multiplier (1.0 = normal, 0.0 = frozen, 72.0 = 72× faster)
     */
    private record TimeState(boolean frozen, float rate) {}

    /** Default state for dimensions that have not been explicitly configured. */
    private static final TimeState DEFAULT_STATE = new TimeState(false, 1.0f);

    /** Frozen/fast-forward state per dimension. */
    private static final Map<ResourceKey<Level>, TimeState> TIME_STATES =
            new ConcurrentHashMap<>();

    /** Captured day time used to hold a dimension frozen. */
    private static final Map<ResourceKey<Level>, Long> FROZEN_DAY_TIMES =
            new ConcurrentHashMap<>();

    static {
        // Register the per-tick time driver on the NeoForge event bus.
        NeoForge.EVENT_BUS.register(TimeTickHandler.class);
    }

    // ──────────────────────────────────────────────
    //  Absolute time control
    // ──────────────────────────────────────────────

    /**
     * Set the absolute day time of a level.
     *
     * @param level     the server level to modify
     * @param timeOfDay the new day time (0–23999, wraps automatically)
     */
    public static void setTime(ServerLevel level, long timeOfDay) {
        level.setDayTime(timeOfDay);
    }

    /**
     * Set the level's time to midday (1000).
     *
     * @param level the server level to modify
     */
    public static void setDay(ServerLevel level) {
        setTime(level, 1000L);
    }

    /**
     * Set the level's time to midnight (13000).
     *
     * @param level the server level to modify
     */
    public static void setNight(ServerLevel level) {
        setTime(level, 13000L);
    }

    /**
     * Add ticks to the current day time.
     *
     * @param level the server level to modify
     * @param ticks the number of ticks to advance (may be negative)
     */
    public static void addTime(ServerLevel level, long ticks) {
        level.setDayTime(level.getDayTime() + ticks);
    }

    // ──────────────────────────────────────────────
    //  Time rate / freeze control
    // ──────────────────────────────────────────────

    /**
     * Set the game time rate for a level.
     * <p>
     * A rate of {@code 1.0} is normal speed, {@code 0.0} freezes time, and
     * e.g. {@code 72.0} runs 72× faster. Rates below {@code 0} are rejected.
     *
     * @param level the server level to modify
     * @param rate  the desired time multiplier (must be {@code >= 0})
     * @throws IllegalArgumentException if {@code rate} is negative
     */
    public static void setTimeRate(ServerLevel level, float rate) {
        if (rate < 0f) {
            throw new IllegalArgumentException("Time rate must be >= 0, got " + rate);
        }
        var key = level.dimension();
        boolean frozen = rate <= 0f;
        if (frozen) {
            FROZEN_DAY_TIMES.put(key, level.getDayTime());
        } else {
            FROZEN_DAY_TIMES.remove(key);
        }
        TIME_STATES.put(key, new TimeState(frozen, rate));
    }

    /**
     * Toggle the freeze state of a level.
     * <p>
     * If currently frozen, time resumes at the normal rate ({@code 1.0}).
     * If running, time is frozen at its current value (rate set to {@code 0.0}).
     *
     * @param level the server level to toggle
     */
    public static void toggleTimeFreeze(ServerLevel level) {
        var key = level.dimension();
        var current = TIME_STATES.getOrDefault(key, DEFAULT_STATE);
        if (current.frozen()) {
            FROZEN_DAY_TIMES.remove(key);
            TIME_STATES.put(key, new TimeState(false, 1.0f));
        } else {
            FROZEN_DAY_TIMES.put(key, level.getDayTime());
            TIME_STATES.put(key, new TimeState(true, 0.0f));
        }
    }

    /**
     * Check whether a level's time is currently frozen.
     *
     * @param level the server level to query
     * @return {@code true} if time is frozen
     */
    public static boolean isTimeFrozen(ServerLevel level) {
        return TIME_STATES.getOrDefault(level.dimension(), DEFAULT_STATE).frozen();
    }

    /**
     * Get the current day time of a level.
     *
     * @param level the server level to query
     * @return the current day time in ticks
     */
    public static long getDayTime(ServerLevel level) {
        return level.getDayTime();
    }

    // ──────────────────────────────────────────────
    //  Pure helpers (testable without Minecraft runtime)
    // ──────────────────────────────────────────────

    /**
     * Compute the extra ticks to add on top of Minecraft's natural +1/tick
     * increment for a given time rate.
     * <p>
     * The natural daylight cycle advances time by 1 tick per tick. Returning
     * {@code (rate - 1) * tickDelta} therefore yields the net rate:
     * <ul>
     *   <li>{@code rate == 1.0} → {@code 0} extra (normal speed)</li>
     *   <li>{@code rate == 72.0} → {@code +71} extra (72× faster)</li>
     *   <li>{@code rate == 0.0} → {@code -1} extra (cancels the natural increment)</li>
     * </ul>
     *
     * @param rate      the time multiplier
     * @param tickDelta the partial-tick delta for this tick
     * @return the number of extra ticks to apply
     */
    public static long computeExtraTicks(float rate, float tickDelta) {
        return Math.round((rate - 1f) * tickDelta);
    }

    // ──────────────────────────────────────────────
    //  Tick driver
    // ──────────────────────────────────────────────

    /**
     * Static event handler that drives per-dimension time advancement on the
     * server tick. Registered once on the NeoForge event bus.
     */
    private static final class TimeTickHandler {

        private TimeTickHandler() {}

        @SubscribeEvent
        public static void onServerPostTick(ServerTickEvent.Post event) {
            MinecraftServer server = event.getServer();
            for (ServerLevel level : server.getAllLevels()) {
                var key = level.dimension();
                var state = TIME_STATES.getOrDefault(key, DEFAULT_STATE);

                if (state.frozen()) {
                    // Hold time constant: re-apply the captured day time,
                    // cancelling Minecraft's natural daylight increment.
                    Long held = FROZEN_DAY_TIMES.get(key);
                    if (held != null) {
                        level.setDayTime(held);
                    }
                } else if (state.rate() > 0f) {
                    // Server ticks advance exactly 1.0 per tick (no partial delta).
                    long extra = computeExtraTicks(state.rate(), 1.0f);
                    if (extra != 0L) {
                        level.setDayTime(level.getDayTime() + extra);
                    }
                }
                // rate == 0 but not flagged frozen: leave Minecraft's natural
                // increment untouched (behaves as normal speed).
            }
        }
    }
}
