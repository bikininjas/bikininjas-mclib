package com.bikininjas.corelib.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Objective requiring the player to stand within {@code radius} blocks of a
 * target position.
 * <p>
 * Completion is binary: the player is either inside the radius (progress 1.0) or
 * not (progress 0.0). The distance is evaluated on every server tick by
 * {@link ObjectiveTracker}.
 */
public record ReachObjective(
        @NotNull String description,
        @NotNull BlockPos targetPos,
        double radius
) implements Objective {

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return player.distanceToSqr(
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5
        ) <= radius * radius;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        return isComplete(player) ? 1.0f : 0.0f;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        return isComplete(player) ? target() : 0;
    }

    @Override
    public int target() {
        return 1;
    }

    @Override
    public ObjectiveType type() {
        return ObjectiveType.REACH;
    }
}
