package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Objective requiring the player to kill a number of entities of a given type.
 * <p>
 * The live kill count is read from {@link ObjectiveTracker#COUNTS} keyed by the
 * player's UUID and this objective's {@link #description()} (record equality
 * guarantees two identically-built objectives share the same counter bucket).
 */
public record KillObjective(
        @NotNull String description,
        @NotNull EntityType<?> entityType,
        int target
) implements Objective {

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return progress(player) >= 1.0f;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        return (float) progressValue(player) / (float) target;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        Integer count = ObjectiveTracker.COUNTS
                .getOrDefault(player.getUUID(), java.util.Map.of())
                .get(description);
        return Math.min(count == null ? 0 : count, target);
    }

    @Override
    public ObjectiveType type() {
        return ObjectiveType.KILL;
    }
}
