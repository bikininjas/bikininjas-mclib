package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Objective: survive for a given duration (in ticks).
 * <p>
 * Progress tracks elapsed ticks since the challenge started.
 */
public record SurvivalObjective(
        @NotNull String description,
        int durationTicks
) implements Objective {

    private static final ConcurrentMap<ServerPlayer, Long> startTimes = new ConcurrentHashMap<>();

    public SurvivalObjective {
        Objects.requireNonNull(description, "description must not be null");
    }

    /**
     * Record the start time for a player (called when a challenge begins).
     */
    public static void start(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        startTimes.put(player, player.serverLevel().getGameTime());
    }

    /**
     * Stop tracking a player.
     */
    public static void stop(@NotNull ServerPlayer player) {
        startTimes.remove(player);
    }

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return progressValue(player) >= durationTicks;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        return durationTicks > 0
                ? Math.min(1.0f, (float) progressValue(player) / durationTicks)
                : 0.0f;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        var start = startTimes.get(player);
        if (start == null) return 0;
        return (int) (player.serverLevel().getGameTime() - start);
    }

    @Override
    public int target() {
        return durationTicks;
    }

    @Override
    public @NotNull ObjectiveType type() {
        return ObjectiveType.SURVIVE;
    }
}
