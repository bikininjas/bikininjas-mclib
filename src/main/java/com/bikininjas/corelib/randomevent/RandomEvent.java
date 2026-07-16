package com.bikininjas.corelib.randomevent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for a random event that can be triggered by {@link RandomEventManager}.
 * <p>
 * Implementations must provide a human-readable {@link #name()} and a
 * selection {@link #weight()} (0 = never selected).
 * <p>
 * For convenience, use {@link RandomEventManager#register(RandomEvent, String)}
 * to assign a name instead of implementing it yourself.
 */
public interface RandomEvent {

    /**
     * Execute this event at the given level and origin.
     */
    void execute(@NotNull ServerLevel level, @NotNull Vec3 origin);

    /**
     * Human-readable name for this event. Used for display and filtering.
     * <p>
     * Concrete implementations must return a stable, non-reflective name.
     * For anonymous or lambda-like events, use
     * {@link RandomEventManager#register(RandomEvent, String)} with an explicit name.
     */
    @NotNull String name();

    /**
     * Selection weight. Higher values = more likely to be chosen.
     * Returning 0 excludes this event from random selection.
     */
    default int weight() {
        return 1;
    }
}
