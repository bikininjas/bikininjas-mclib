package com.bikininjas.corelib.objective;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event-driven, per-player challenge tracker.
 * <p>
 * This is a stateless utility: there is no singleton. All state lives in static
 * maps keyed by player {@link UUID}. The tracker is wired to the NeoForge event
 * bus through the nested {@link ObjectiveHandler} class, registered once via the
 * static initialiser block.
 *
 * <p>Lifecycle:
 * <ul>
 *     <li>{@link #startChallenge(ServerPlayer, Challenge)} assigns the challenge's
 *         objectives to a player and records the start tick.</li>
 *     <li>Kill / collect counts accumulate in {@link #COUNTS} as the relevant
 *         events fire.</li>
 *     <li>Reach / survival progress is evaluated every server tick.</li>
 *     <li>{@link #stopChallenge(ServerPlayer)} clears all state for a player.</li>
 * </ul>
 */
public final class ObjectiveTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectiveTracker.class);

    /** Player UUID → list of active objectives. */
    public static final Map<UUID, List<Objective>> objectives = new ConcurrentHashMap<>();

    /** Player UUID → (objective description → current count). */
    public static final Map<UUID, Map<String, Integer>> COUNTS = new ConcurrentHashMap<>();

    /** Player UUID → server tick at which the challenge started. */
    public static final Map<UUID, Long> START_TIMES = new ConcurrentHashMap<>();

    /** Tick counter advanced by the server tick handler. */
    private static volatile long currentTick = 0L;

    private ObjectiveTracker() {
        // Static-only utility. No instances.
    }

    static {
        NeoForge.EVENT_BUS.register(ObjectiveHandler.class);
    }

    /**
     * @return the current server tick counter (monotonic, advanced each tick).
     */
    public static long currentTick() {
        return currentTick;
    }

    /**
     * Begins tracking a challenge for the given player.
     *
     * @param player   the player to track; never {@code null}.
     * @param challenge the challenge whose objectives are assigned; never {@code null}.
     */
    public static void startChallenge(@NotNull ServerPlayer player, @NotNull Challenge challenge) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(challenge, "challenge");
        UUID id = player.getUUID();
        objectives.put(id, List.copyOf(challenge.objectives()));
        COUNTS.put(id, new ConcurrentHashMap<>());
        START_TIMES.put(id, currentTick);
        LOGGER.debug("Started challenge '{}' for {}", challenge.name(), id);
    }

    /**
     * Stops tracking and clears all state for the given player.
     *
     * @param player the player to stop tracking; never {@code null}.
     */
    public static void stopChallenge(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        UUID id = player.getUUID();
        objectives.remove(id);
        COUNTS.remove(id);
        START_TIMES.remove(id);
        LOGGER.debug("Stopped challenge tracking for {}", id);
    }

    /**
     * @param player the player whose progress is queried; never {@code null}.
     * @return the average progress across all active objectives in {@code 0.0f – 1.0f},
     *         or {@code 0.0f} when the player has no active objectives.
     */
    public static float getProgress(ServerPlayer player) {
        if (player == null) {
            return 0.0f;
        }
        List<Objective> objs = objectives.get(player.getUUID());
        if (objs == null || objs.isEmpty()) {
            return 0.0f;
        }
        float sum = 0.0f;
        for (Objective o : objs) {
            sum += o.progress(player);
        }
        return sum / (float) objs.size();
    }

    /**
     * @param player the player whose objectives are requested; never {@code null}.
     * @return an immutable copy of the player's active objectives (empty if none).
     */
    public static List<Objective> getObjectives(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        List<Objective> objs = objectives.get(player.getUUID());
        return objs == null ? List.of() : List.copyOf(objs);
    }

    /**
     * @param player the player to check; never {@code null}.
     * @return {@code true} if the player currently has active objectives.
     */
    public static boolean isTracking(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        List<Objective> objs = objectives.get(player.getUUID());
        return objs != null && !objs.isEmpty();
    }

    /**
     * Increments the stored count for an objective description belonging to a player.
     */
    private static void incrementCount(@NotNull UUID playerId, @NotNull String description) {
        COUNTS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .merge(description, 1, Integer::sum);
    }

    /**
     * Static event handler registered on the NeoForge event bus. All handlers are
     * static so no instance is required.
     */
    private static final class ObjectiveHandler {

        private ObjectiveHandler() {
            // Static handler container.
        }

        @SubscribeEvent
        static void onLivingDeath(@NotNull LivingDeathEvent event) {
            net.minecraft.world.entity.LivingEntity victim = event.getEntity();
            if (victim == null) {
                return;
            }
            EntityType<?> killedType = victim.getType();
            // Route to every tracking player: increment matching KillObjectives.
            for (Map.Entry<UUID, List<Objective>> entry : objectives.entrySet()) {
                UUID playerId = entry.getKey();
                for (Objective o : entry.getValue()) {
                    if (o instanceof KillObjective k && k.entityType() == killedType) {
                        incrementCount(playerId, k.description());
                    }
                }
            }
        }

        @SubscribeEvent
        static void onEntityItemPickup(@NotNull ItemEntityPickupEvent.Post event) {
            if (!(event.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            ItemEntity itemEntity = event.getItemEntity();
            if (itemEntity == null) {
                return;
            }
            ItemStack stack = itemEntity.getItem();
            if (stack == null || stack.isEmpty()) {
                return;
            }
            net.minecraft.world.item.Item item = stack.getItem();
            UUID playerId = player.getUUID();
            for (Objective o : objectives.getOrDefault(playerId, List.of())) {
                if (o instanceof CollectObjective c && c.item() == item) {
                    incrementCount(playerId, c.description());
                }
            }
        }

        @SubscribeEvent
        static void onServerTick(@NotNull ServerTickEvent.Post event) {
            currentTick = event.getServer().getTickCount();
            // Reach / survival progress is read lazily via progress(); this tick hook
            // exists so the counter advances and completion can be polled. Completion
            // checks are performed by callers via getProgress()/isComplete().
        }
    }
}
