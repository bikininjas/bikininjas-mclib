package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Objective: kill a certain number of a specific entity type.
 */
public record KillObjective(
        @NotNull String description,
        @NotNull EntityType<?> targetType,
        int target
) implements Objective {

    private static final ConcurrentMap<String, ConcurrentMap<ServerPlayer, Integer>> killCounts = new ConcurrentHashMap<>();
    private static final Map<String, EntityType<?>> activeTargets = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(KillHandler.class);
    }

    public KillObjective {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        activeTargets.put(description, targetType);
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
        return killCounts
                .getOrDefault(description, new ConcurrentHashMap<>())
                .getOrDefault(player, 0);
    }

    @Override
    public @NotNull ObjectiveType type() {
        return ObjectiveType.KILL;
    }

    // -- Handler -------------------------------------------------------------

    private static final class KillHandler {
        private KillHandler() {
        }

        @SubscribeEvent
        static void onKill(@NotNull LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

            var killedType = event.getEntity().getType();
            for (var entry : activeTargets.entrySet()) {
                if (entry.getValue().equals(killedType)) {
                    killCounts.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>())
                            .merge(killer, 1, Integer::sum);
                }
            }
        }
    }
}
