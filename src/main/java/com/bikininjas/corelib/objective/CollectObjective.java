package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Objective: collect a certain number of a specific item.
 */
public record CollectObjective(
        @NotNull String description,
        @NotNull Item targetItem,
        int target
) implements Objective {

    private static final ConcurrentMap<String, ConcurrentMap<ServerPlayer, Integer>> collectCounts = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Item, String> collectTargets = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(CollectHandler.class);
    }

    public CollectObjective {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(targetItem, "targetItem must not be null");
        collectTargets.put(targetItem, description);
    }

    @Override
    public boolean isComplete(@NotNull ServerPlayer player) {
        return progressValue(player) >= target;
    }

    @Override
    public float progress(@NotNull ServerPlayer player) {
        return target > 0 ? Math.min(1.0f, (float) progressValue(player) / target) : 0.0f;
    }

    @Override
    public int progressValue(@NotNull ServerPlayer player) {
        return collectCounts
                .getOrDefault(description, new ConcurrentHashMap<>())
                .getOrDefault(player, 0);
    }

    @Override
    public @NotNull ObjectiveType type() {
        return ObjectiveType.COLLECT;
    }

    // -- Cleanup --------------------------------------------------------------

    /**
     * Remove all tracking data for a given description (called when a challenge ends).
     */
    public static void cleanup(@NotNull String description) {
        Objects.requireNonNull(description, "description must not be null");
        collectTargets.entrySet().removeIf(e -> e.getValue().equals(description));
        collectCounts.remove(description);
    }

    // -- Handler -------------------------------------------------------------

    private static final class CollectHandler {
        private CollectHandler() {
        }

        @SubscribeEvent
        static void onPickup(@NotNull ItemEntityPickupEvent.Post event) {
            var player = event.getPlayer();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            var stack = event.getItemEntity().getItem();
            var item = stack.getItem();

            var descKey = collectTargets.get(item);
            if (descKey != null) {
                collectCounts.computeIfAbsent(descKey, k -> new ConcurrentHashMap<>())
                        .merge(serverPlayer, stack.getCount(), Integer::sum);
            }
        }
    }
}
