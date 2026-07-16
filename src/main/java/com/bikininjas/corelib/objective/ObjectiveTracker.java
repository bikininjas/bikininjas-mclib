package com.bikininjas.corelib.objective;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    /** Player UUID → name of the active challenge. */
    public static final Map<UUID, String> ACTIVE_CHALLENGE_NAMES = new ConcurrentHashMap<>();

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
        ACTIVE_CHALLENGE_NAMES.put(id, challenge.name());
        LOGGER.debug("Started challenge '{}' for {}", challenge.name(), id);
        saveToPlayer(player);
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
        ACTIVE_CHALLENGE_NAMES.remove(id);
        LOGGER.debug("Stopped challenge tracking for {}", id);
        saveToPlayer(player);
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
     * Check whether all active objectives for a player have been completed.
     *
     * @param player the player to check; never {@code null}.
     * @return {@code true} if the player has active objectives and all of them
     *         report {@link Objective#isComplete(ServerPlayer) complete}.
     */
    public static boolean isChallengeComplete(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        List<Objective> objs = objectives.get(player.getUUID());
        return objs != null && !objs.isEmpty()
                && objs.stream().allMatch(o -> o.isComplete(player));
    }

    /**
     * @param player the player to query; never {@code null}.
     * @return the wall-clock seconds elapsed since the challenge started,
     *         or {@code 0} if the player is not tracking a challenge.
     */
    public static long getElapsedSeconds(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Long start = START_TIMES.get(player.getUUID());
        if (start == null) {
            return 0L;
        }
        return (currentTick - start) / 20L;
    }

    /**
     * @param player the player to query; never {@code null}.
     * @return the name of the player's active challenge, or {@code null} if
     *         the player is not tracking a challenge.
     */
    @Nullable
    public static String getActiveChallengeName(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return ACTIVE_CHALLENGE_NAMES.get(player.getUUID());
    }

    /**
     * Increments the stored count for an objective description belonging to a player.
     */
    private static void incrementCount(@NotNull UUID playerId, @NotNull String description) {
        COUNTS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .merge(description, 1, Integer::sum);
    }

    // ──────────────────────────────────────────────
    //  NBT persistence
    // ──────────────────────────────────────────────

    private static final String TAG_ACTIVE_CHALLENGE = "obi_active_challenge";
    private static final String TAG_START_TICK       = "obi_start_tick";
    private static final String TAG_ELAPSED_SECONDS  = "obi_elapsed_seconds";
    private static final String TAG_COUNTS           = "obi_counts";

    /**
     * Save active challenge state to the player's persistent data.
     * Called automatically on challenge start/stop and on player logout.
     *
     * @param player the player whose data to save; never {@code null}.
     */
    public static void saveToPlayer(@NotNull ServerPlayer player) {
        UUID id = player.getUUID();
        CompoundTag data = player.getPersistentData();

        String challenge = ACTIVE_CHALLENGE_NAMES.get(id);
        if (challenge != null) {
            data.putString(TAG_ACTIVE_CHALLENGE, challenge);
            long startTick = START_TIMES.getOrDefault(id, 0L);
            long elapsedSec = startTick > 0 ? (currentTick - startTick) / 20L : 0L;
            data.putLong(TAG_START_TICK, startTick);
            data.putLong(TAG_ELAPSED_SECONDS, elapsedSec);
        } else {
            data.remove(TAG_ACTIVE_CHALLENGE);
            data.remove(TAG_START_TICK);
            data.remove(TAG_ELAPSED_SECONDS);
        }

        // Save objective counts
        Map<String, Integer> playerCounts = COUNTS.get(id);
        if (playerCounts != null && !playerCounts.isEmpty()) {
            CompoundTag countsTag = new CompoundTag();
            for (var entry : playerCounts.entrySet()) {
                countsTag.putInt(entry.getKey(), entry.getValue());
            }
            data.put(TAG_COUNTS, countsTag);
        } else {
            data.remove(TAG_COUNTS);
        }
    }

    /**
     * Load active challenge state from the player's persistent data.
     * Called automatically on player login.
     *
     * @param player the player whose data to load; never {@code null}.
     */
    public static void loadFromPlayer(@NotNull ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        UUID id = player.getUUID();

        // Restore challenge only if the definition still exists
        if (data.contains(TAG_ACTIVE_CHALLENGE)) {
            String challengeName = data.getString(TAG_ACTIVE_CHALLENGE);
            ChallengeDefinition def = ChallengeRegistry.get(challengeName);
            if (def != null && ChallengeRegistry.areModsLoaded(def)) {
                long startTick = data.getLong(TAG_START_TICK);
                long elapsedSec = data.getLong(TAG_ELAPSED_SECONDS);
                // Adjust start tick so elapsed time is preserved after restart
                long correctedStart = currentTick - (elapsedSec * 20L);
                if (correctedStart < 0) correctedStart = 0;

                ACTIVE_CHALLENGE_NAMES.put(id, challengeName);
                START_TIMES.put(id, correctedStart);
                objectives.put(id, new ArrayList<>(def.objectives()));
                LOGGER.debug("Restored challenge '{}' for {} (elapsed: {}s)", challengeName, id, elapsedSec);
            } else {
                data.remove(TAG_ACTIVE_CHALLENGE);
                data.remove(TAG_START_TICK);
                data.remove(TAG_ELAPSED_SECONDS);
                LOGGER.debug("Challenge '{}' no longer available, cleared for {}", challengeName, id);
            }
        }

        // Restore objective counts
        if (data.contains(TAG_COUNTS)) {
            CompoundTag countsTag = data.getCompound(TAG_COUNTS);
            Map<String, Integer> playerCounts = new ConcurrentHashMap<>();
            for (String key : countsTag.getAllKeys()) {
                playerCounts.put(key, countsTag.getInt(key));
            }
            COUNTS.put(id, playerCounts);
        }
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
        static void onPlayerLogin(@NotNull PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                loadFromPlayer(player);
            }
        }

        @SubscribeEvent
        static void onPlayerLogout(@NotNull PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                saveToPlayer(player);
            }
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
            MinecraftServer server = event.getServer();
            currentTick = server.getTickCount();

            // Check completion every tick; send action bar every 20 ticks (~1s).
            boolean tickSecond = (currentTick & 0xF) == 0; // tick % 20 == 0

            for (Map.Entry<UUID, List<Objective>> entry : objectives.entrySet()) {
                UUID playerId = entry.getKey();
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    continue;
                }

                // Auto-stop when all objectives are complete.
                if (entry.getValue().stream().allMatch(o -> o.isComplete(player))) {
                    String name = ACTIVE_CHALLENGE_NAMES.getOrDefault(playerId, "?");
                    stopChallenge(player);
                    player.displayClientMessage(
                            Component.literal("§a✔ Challenge '").append(name)
                                    .append(Component.literal("' completed!")), false);
                    continue;
                }

                // Timer action bar every second.
                if (tickSecond) {
                    long elapsed = getElapsedSeconds(player);
                    long mins = elapsed / 60;
                    long secs = elapsed % 60;
                    float prog = getProgress(player);
                    int pct = Math.round(prog * 100.0f);
                    String cname = ACTIVE_CHALLENGE_NAMES.getOrDefault(playerId, "?");

                    player.displayClientMessage(Component.literal(
                            String.format("§e⏱ §f%s §7| §a%d%% §7| §e%d:%02d",
                                    cname, pct, mins, secs)), true);
                }
            }
        }
    }
}
