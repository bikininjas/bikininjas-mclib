package com.bikininjas.corelib.randomevent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Server-side scheduler and registry for {@link RandomEvent}s.
 * <p>
 * This is a singleton ({@link #getInstance()}). Events are registered into a
 * named pool and, while enabled, one is fired at a random interval (in ticks)
 * chosen between {@link #minInterval} and {@link #maxInterval}. Selection is
 * weighted by each event's {@link RandomEvent#weight()}.
 * <p>
 * The manager self-subscribes to {@link ServerTickEvent.Post} on the NeoForge
 * event bus to drive the cooldown. If the event bus is unavailable (e.g. a
 * pure unit-test environment without a Minecraft runtime) tick scheduling is
 * silently disabled but the manager remains fully usable for manual firing.
 */
public final class RandomEventManager {

    // ──────────────────────────────────────────────
    //  Singleton
    // ──────────────────────────────────────────────

    private static RandomEventManager instance;

    /**
     * Get the global {@code RandomEventManager} instance, creating it on first use.
     *
     * @return the singleton manager
     */
    public static RandomEventManager getInstance() {
        if (instance == null) {
            instance = new RandomEventManager();
        }
        return instance;
    }

    // ──────────────────────────────────────────────
    //  State
    // ──────────────────────────────────────────────

    /** Registered events keyed by name. Insertion order is preserved for stable iteration. */
    private final Map<String, RandomEvent> events = new LinkedHashMap<>();

    private boolean enabled = true;

    /** Minimum cooldown between timed events, in ticks (default 600 = 30s). */
    private int minInterval = 600;

    /** Maximum cooldown between timed events, in ticks (default 2400 = 2m). */
    private int maxInterval = 2400;

    /** Remaining ticks until the next timed event fires. */
    private int currentCooldown = 0;

    private final Random random = new Random();

    /** Last known server level, captured from tick events. */
    private ServerLevel targetLevel = null;

    /**
     * Package-private constructor. Use {@link #getInstance()} to obtain the singleton.
     */
    RandomEventManager() {
        this.currentCooldown = minInterval;
        try {
            NeoForge.EVENT_BUS.register(this);
        } catch (Throwable ignored) {
            // Event bus unavailable outside a Minecraft runtime — tick scheduling disabled.
        }
    }

    // ──────────────────────────────────────────────
    //  Registration
    // ──────────────────────────────────────────────

    /**
     * Register an event using its {@link RandomEvent#name()} as the key.
     *
     * @param event the event to add to the pool
     * @return this manager, for chaining
     */
    public RandomEventManager register(RandomEvent event) {
        return register(event, event.name());
    }

    /**
     * Register an event under an explicit name.
     *
     * @param event the event to add to the pool
     * @param name  the registry key (overrides {@link RandomEvent#name()})
     * @return this manager, for chaining
     */
    public RandomEventManager register(RandomEvent event, String name) {
        events.put(name, event);
        return this;
    }

    /**
     * Remove an event from the pool by name.
     *
     * @param name the registry key of the event to remove
     */
    public void remove(String name) {
        events.remove(name);
    }

    // ──────────────────────────────────────────────
    //  Enable / interval control
    // ──────────────────────────────────────────────

    /**
     * Enable or disable the whole random-event system.
     * <p>
     * When disabled, neither timed nor manual events fire.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return whether the system is currently enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the cooldown range (in ticks) used for timed events.
     *
     * @param min minimum cooldown in ticks (must be {@code >= 0})
     * @param max maximum cooldown in ticks
     */
    public void setInterval(int min, int max) {
        int lo = Math.max(0, Math.min(min, max));
        int hi = Math.max(0, Math.max(min, max));
        this.minInterval = lo;
        this.maxInterval = hi;
        resetCooldown();
    }

    /**
     * @return the current minimum cooldown in ticks
     */
    public int getMinInterval() {
        return minInterval;
    }

    /**
     * @return the current maximum cooldown in ticks
     */
    public int getMaxInterval() {
        return maxInterval;
    }

    // ──────────────────────────────────────────────
    //  Firing
    // ──────────────────────────────────────────────

    /**
     * Manually fire a single random event from the pool, respecting weights.
     * <p>
     * The origin is chosen from a random online player's position; if no level
     * or players are available, {@link Vec3#ZERO} is used.
     *
     * @param level the server level to run the event in (may be {@code null})
     */
    public void fireRandomEvent(ServerLevel level) {
        if (!enabled || events.isEmpty()) {
            return;
        }
        RandomEvent event = selectRandomEvent();
        if (event == null) {
            return;
        }
        event.execute(level, computeOrigin(level));
    }

    /**
     * Fire a specific registered event by name.
     *
     * @param name   the registry key of the event to fire
     * @param level  the server level to run the event in (may be {@code null})
     * @param origin the world-space origin to pass to the event
     */
    public void fireEvent(String name, ServerLevel level, Vec3 origin) {
        if (!enabled) {
            return;
        }
        RandomEvent event = events.get(name);
        if (event != null) {
            event.execute(level, origin);
        }
    }

    /**
     * Select a random event from the pool using weighted selection.
     * <p>
     * Events with higher {@link RandomEvent#weight()} are proportionally more
     * likely to be returned. Events with a weight of {@code 0} are never chosen.
     *
     * @return the selected event, or {@code null} if the pool is empty
     */
    public RandomEvent selectRandomEvent() {
        if (events.isEmpty()) {
            return null;
        }
        int total = 0;
        for (RandomEvent e : events.values()) {
            total += Math.max(0, e.weight());
        }
        if (total <= 0) {
            // No positive weights — fall back to uniform selection.
            int idx = random.nextInt(events.size());
            return new ArrayList<>(events.values()).get(idx);
        }
        int roll = random.nextInt(total);
        for (RandomEvent e : events.values()) {
            roll -= Math.max(0, e.weight());
            if (roll < 0) {
                return e;
            }
        }
        return events.values().iterator().next();
    }

    // ──────────────────────────────────────────────
    //  Introspection
    // ──────────────────────────────────────────────

    /**
     * @return an immutable list of all registered event names
     */
    public List<String> getAllEvents() {
        return new ArrayList<>(events.keySet());
    }

    /**
     * @return the number of registered events
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * Reset the manager to a clean state: clears all events, re-enables the
     * system, restores the default interval, and resets the cooldown.
     * <p>
     * Primarily useful for tests and for reloading configuration.
     */
    public void reset() {
        events.clear();
        enabled = true;
        minInterval = 600;
        maxInterval = 2400;
        currentCooldown = minInterval;
        targetLevel = null;
    }

    // ──────────────────────────────────────────────
    //  Internal tick handling
    // ──────────────────────────────────────────────

    /**
     * Drive the cooldown on every server tick. When the cooldown reaches zero a
     * random event is fired and the cooldown is randomised again.
     *
     * @param event the server tick event
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level == null) {
                var levels = server.getAllLevels().iterator();
                level = levels.hasNext() ? levels.next() : null;
            }
            this.targetLevel = level;
        }

        if (!enabled) {
            return;
        }
        if (currentCooldown > 0) {
            currentCooldown--;
            return;
        }
        fireRandomEvent(targetLevel);
        resetCooldown();
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────

    private void resetCooldown() {
        if (maxInterval <= minInterval) {
            currentCooldown = minInterval;
        } else {
            currentCooldown = minInterval + random.nextInt(maxInterval - minInterval + 1);
        }
    }

    private Vec3 computeOrigin(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return Vec3.ZERO;
        }
        var players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return Vec3.ZERO;
        }
        var player = players.get(random.nextInt(players.size()));
        return player.position();
    }
}
