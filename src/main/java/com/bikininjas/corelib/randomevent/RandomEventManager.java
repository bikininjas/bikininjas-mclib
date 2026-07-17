package com.bikininjas.corelib.randomevent;

import com.google.common.base.Preconditions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton engine for scheduling and firing random events with configurable
 * cooldown intervals and weighted selection.
 * <p>
 * Automatically registered on the NeoForge event bus via static initializer.
 */
public final class RandomEventManager {

    private static final RandomEventManager INSTANCE = new RandomEventManager();
    private static final Random RNG = new Random();

    private final List<Entry> events = new CopyOnWriteArrayList<>();
    private boolean enabled = true;
    private int cooldownMin = 600;   // 30 seconds at 20 ticks/s
    private int cooldownMax = 1800;  // 90 seconds
    private int ticksUntilNext = 0;

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private RandomEventManager() {
    }

    /**
     * Get the singleton instance.
     */
    public static @NotNull RandomEventManager getInstance() {
        return INSTANCE;
    }

    // -- Registration --------------------------------------------------------

    /**
     * Register an event with an auto-generated name (class simple name).
     */
    public void register(@NotNull RandomEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        events.add(new Entry(event.name(), event));
    }

    /**
     * Register an event with an explicit key name.
     */
    public void register(@NotNull RandomEvent event, @NotNull String name) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(name, "name must not be null");
        events.add(new Entry(name, event));
    }

    /**
     * Remove an event by name.
     */
    public void remove(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        events.removeIf(e -> e.name.equals(name));
    }

    // -- Configuration -------------------------------------------------------

    /**
     * Enable or disable the random event engine.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set the cooldown interval range (in ticks) between random events.
     */
    public void setInterval(int min, int max) {
        Preconditions.checkArgument(min >= 0, "min must be >= 0");
        Preconditions.checkArgument(max >= min, "max must be >= min");
        this.cooldownMin = min;
        this.cooldownMax = max;
    }

    // -- Firing --------------------------------------------------------------

    /**
     * Manually fire a random event (ignores cooldown).
     * Selects from registered events using weighted random selection.
     *
     * @return the event that was fired, or null if no events are registered
     */
    public @Nullable RandomEvent fireRandomEvent(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        var selected = selectRandomEvent();
        if (selected != null) {
            // Use a random online player's position, falling back to world spawn
            var server = level.getServer();
            java.util.List<net.minecraft.server.level.ServerPlayer> players = server != null
                    ? server.getPlayerList().getPlayers()
                    : java.util.Collections.emptyList();
            Vec3 origin;
            if (!players.isEmpty()) {
                var player = players.get(RNG.nextInt(players.size()));
                origin = player.position();
            } else {
                origin = Vec3.atCenterOf(level.getSharedSpawnPos());
            }
            selected.execute(level, origin);
        }
        return selected;
    }

    /**
     * Fire a specific event by name, ignoring cooldown.
     *
     * @return the event that was fired, or null if not found
     */
    public @Nullable RandomEvent fireEvent(@NotNull String name, @NotNull ServerLevel level, @NotNull Vec3 origin) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(origin, "origin must not be null");
        for (var entry : events) {
            if (entry.name.equals(name)) {
                entry.event.execute(level, origin);
                return entry.event;
            }
        }
        return null;
    }

    /**
     * Perform weighted random selection from all registered events.
     *
     * @return the selected event, or null if none registered
     */
    public @Nullable RandomEvent selectRandomEvent() {
        if (events.isEmpty()) {
            return null;
        }
        int totalWeight = events.stream().mapToInt(e -> e.event.weight()).sum();
        if (totalWeight <= 0) {
            return null;
        }
        int roll = RNG.nextInt(totalWeight);
        int cumulative = 0;
        for (var entry : events) {
            cumulative += entry.event.weight();
            if (roll < cumulative) {
                return entry.event;
            }
        }
        return events.getLast().event;
    }

    // -- Queries -------------------------------------------------------------

    /**
     * Get all registered event names.
     */
    public @NotNull List<String> getAllEvents() {
        return events.stream().map(e -> e.name).toList();
    }

    /**
     * Get the number of registered events.
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * Reset the manager to its initial state: removes all events and resets cooldown.
     */
    public void reset() {
        events.clear();
        ticksUntilNext = 0;
        enabled = true;
    }

    // -- Internal ------------------------------------------------------------

    private record Entry(String name, RandomEvent event) {
    }

    private int nextCooldown() {
        if (cooldownMax <= cooldownMin) {
            return cooldownMin;
        }
        return cooldownMin + RNG.nextInt(cooldownMax - cooldownMin + 1);
    }

    // -- Event handler -------------------------------------------------------

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onServerTick(@NotNull ServerTickEvent.Post event) {
            var mgr = getInstance();
            if (!mgr.enabled || mgr.events.isEmpty()) {
                return;
            }

            mgr.ticksUntilNext--;
            if (mgr.ticksUntilNext <= 0) {
                mgr.ticksUntilNext = mgr.nextCooldown();
                // Fire events on ALL server levels, not just the overworld
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (var level : server.getAllLevels()) {
                        if (level != null) {
                            mgr.fireRandomEvent(level);
                        }
                    }
                }
            }
        }
    }
}
