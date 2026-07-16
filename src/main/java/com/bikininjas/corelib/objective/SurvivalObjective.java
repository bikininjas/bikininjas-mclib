package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Objective requiring the player to survive for a number of ticks since the
 * challenge started.
 * <p>
 * The elapsed tick count is derived from the challenge start time recorded in
 * {@link ObjectiveTracker#START_TIMES} and the current server tick counter,
 * evaluated on every server tick. Progress is capped at {@code 1.0f}.
 */
public record SurvivalObjective(
        @NotNull String description,
        int durationTicks
) implements Objective {

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return progress(player) >= 1.0f;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        long start = ObjectiveTracker.START_TIMES.getOrDefault(player.getUUID(), 0L);
        if (start == 0L) {
            return 0.0f;
        }
        long elapsed = ObjectiveTracker.currentTick() - start;
        if (elapsed <= 0) {
            return 0.0f;
        }
        return (float) Math.min(elapsed, durationTicks) / (float) durationTicks;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        long start = ObjectiveTracker.START_TIMES.getOrDefault(player.getUUID(), 0L);
        if (start == 0L) {
            return 0;
        }
        long elapsed = ObjectiveTracker.currentTick() - start;
        return (int) Math.max(0, Math.min(elapsed, durationTicks));
    }

    @Override
    public int target() {
        return durationTicks;
    }

    @Override
    public ObjectiveType type() {
        return ObjectiveType.SURVIVE;
    }
}
