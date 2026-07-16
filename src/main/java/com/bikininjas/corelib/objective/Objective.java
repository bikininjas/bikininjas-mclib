package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Sealed contract describing a single, trackable challenge objective.
 * <p>
 * Every objective exposes a human-readable {@link #description()}, a completion
 * threshold via {@link #target()}, and live progress queries against a given
 * {@link ServerPlayer}. Progress is normalised to the {@code 0.0f – 1.0f} range
 * so that heterogeneous objective kinds (kills, collection, proximity, survival)
 * can be averaged into a single challenge completion percentage.
 *
 * <p>Permitted implementations (exhaustive):
 * <ul>
 *     <li>{@link KillObjective}</li>
 *     <li>{@link CollectObjective}</li>
 *     <li>{@link ReachObjective}</li>
 *     <li>{@link SurvivalObjective}</li>
 * </ul>
 */
public sealed interface Objective permits KillObjective, CollectObjective, ReachObjective, SurvivalObjective {

    /**
     * The category of this objective. Used by {@link ObjectiveTracker} to route
     * the correct event handler (death, pickup, tick) to the right implementation.
     */
    enum ObjectiveType {
        /** Kill a number of entities of a given type. */
        KILL,
        /** Collect a number of items of a given type. */
        COLLECT,
        /** Reach a position within a given radius. */
        REACH,
        /** Survive for a number of ticks. */
        SURVIVE
    }

    /**
     * @return a human-readable description of what the player must accomplish.
     */
    String description();

    /**
     * @param player the player whose progress is queried; never {@code null}.
     * @return {@code true} when the player's progress has reached {@link #target()}.
     */
    boolean isComplete(ServerPlayer player);

    /**
     * @param player the player whose progress is queried; never {@code null}.
     * @return normalised progress in the range {@code 0.0f} (none) to {@code 1.0f} (done).
     */
    float progress(ServerPlayer player);

    /**
     * @param player the player whose progress is queried; never {@code null}.
     * @return the current absolute count toward the objective, clamped to {@code 0..target()}.
     */
    int progressValue(ServerPlayer player);

    /**
     * @return the absolute count required for completion.
     */
    int target();

    /**
     * @return the {@link ObjectiveType} categorising this objective.
     */
    ObjectiveType type();
}
