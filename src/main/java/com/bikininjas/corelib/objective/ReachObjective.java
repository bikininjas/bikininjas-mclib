package com.bikininjas.corelib.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Objective: reach a specific location within a given radius.
 */
public record ReachObjective(
        @NotNull String description,
        @NotNull BlockPos targetPos,
        double radius
) implements Objective {

    public ReachObjective {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(targetPos, "targetPos must not be null");
    }

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return player.position().distanceToSqr(
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5
        ) <= radius * radius;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        return isComplete(player) ? 1.0f : 0.0f;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        return isComplete(player) ? 1 : 0;
    }

    @Override
    public int target() {
        return 1;
    }

    @Override
    public @NotNull ObjectiveType type() {
        return ObjectiveType.REACH;
    }
}
