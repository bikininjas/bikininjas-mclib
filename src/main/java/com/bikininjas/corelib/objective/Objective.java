package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for challenge objectives.
 * <p>
 * Permitted implementations: {@link KillObjective}, {@link CollectObjective},
 * {@link ReachObjective}, {@link SurvivalObjective}.
 */
public sealed interface Objective permits KillObjective, CollectObjective, ReachObjective, SurvivalObjective {

    /**
     * Human-readable description of this objective.
     */
    @NotNull String description();

    /**
     * Check whether this objective is complete for the given player.
     */
    boolean isComplete(@NotNull ServerPlayer player);

    /**
     * Get the progress as a fraction 0.0–1.0.
     */
    float progress(@NotNull ServerPlayer player);

    /**
     * Get the current progress value.
     */
    int progressValue(@NotNull ServerPlayer player);

    /**
     * Get the target value required for completion.
     */
    int target();

    /**
     * Get the objective type.
     */
    @NotNull ObjectiveType type();
}
