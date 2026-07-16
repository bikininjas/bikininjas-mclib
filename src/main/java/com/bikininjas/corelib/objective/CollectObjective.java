package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

/**
 * Objective requiring the player to collect a number of items of a given type.
 * <p>
 * The live collected count is read from {@link ObjectiveTracker#COUNTS} keyed by
 * the player's UUID and this objective's {@link #description()}. The count is
 * incremented by the pickup event handler in {@link ObjectiveTracker}.
 */
public record CollectObjective(
        @NotNull String description,
        @NotNull Item item,
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
        return ObjectiveType.COLLECT;
    }
}
