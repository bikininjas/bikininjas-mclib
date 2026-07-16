package com.bikininjas.corelib.objective;

import com.bikininjas.corelib.message.MessageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks active challenges and objectives per player.
 * <p>
 * Thread-safe. All methods static. Registers event handlers on the NeoForge event bus.
 */
public final class ObjectiveTracker {

    private static final Map<ServerPlayer, ChallengeState> activeChallenges = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(ObjectiveHandler.class);
    }

    private ObjectiveTracker() {
    }

    // -- Challenge lifecycle -------------------------------------------------

    /**
     * Start a challenge for a player.
     */
    public static void startChallenge(@NotNull ServerPlayer player, @NotNull Challenge challenge) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(challenge, "challenge must not be null");
        var state = new ChallengeState(challenge);
        activeChallenges.put(player, state);

        // Start survival timers
        for (var obj : challenge.objectives()) {
            if (obj instanceof SurvivalObjective survival) {
                SurvivalObjective.start(player);
            }
        }
    }

    /**
     * Stop the active challenge for a player.
     */
    public static void stopChallenge(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        var state = activeChallenges.remove(player);
        if (state != null) {
            SurvivalObjective.stop(player);
        }
    }

    // -- Objective management ------------------------------------------------

    /**
     * Add an objective to the player's active challenge.
     */
    public static void addObjective(@NotNull ServerPlayer player, @NotNull Objective objective) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(objective, "objective must not be null");
        var state = activeChallenges.computeIfAbsent(player,
                p -> new ChallengeState(new Challenge("custom", List.of(), 0)));
        state.customObjectives.add(objective);
    }

    /**
     * Remove an objective from a player's active challenge by description.
     */
    public static void removeObjective(@NotNull ServerPlayer player, @NotNull String description) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(description, "description must not be null");
        var state = activeChallenges.get(player);
        if (state != null) {
            state.customObjectives.removeIf(obj -> obj.description().equals(description));
        }
    }

    // -- Queries -------------------------------------------------------------

    /**
     * Check if a player has an active challenge.
     */
    public static boolean isTracking(@NotNull ServerPlayer player) {
        return activeChallenges.containsKey(player);
    }

    /**
     * Get the overall progress of the active challenge (0.0–1.0).
     */
    public static float getProgress(@NotNull ServerPlayer player) {
        var state = activeChallenges.get(player);
        if (state == null) return 0.0f;
        var objs = getAllObjectives(state);
        if (objs.isEmpty()) return 0.0f;
        return (float) objs.stream().mapToDouble(obj -> obj.progress(player)).average().orElse(0.0);
    }

    /**
     * Get the elapsed seconds since the challenge started.
     */
    public static long getElapsedSeconds(@NotNull ServerPlayer player) {
        var state = activeChallenges.get(player);
        if (state == null) return 0;
        if (state.startTick == 0) return 0;
        return (player.serverLevel().getGameTime() - state.startTick) / 20;
    }

    /**
     * Get the current server tick. Used to force class loading.
     */
    public static long currentTick() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        var overworld = server.overworld();
        return overworld != null ? overworld.getGameTime() : 0;
    }

    /**
     * Get the active challenge name for a player.
     */
    public static @NotNull String getActiveChallengeName(@NotNull ServerPlayer player) {
        var state = activeChallenges.get(player);
        return state != null ? state.challenge.name() : "";
    }

    /**
     * Get the objectives for a player's active challenge.
     */
    public static @NotNull List<Objective> getObjectives(@NotNull ServerPlayer player) {
        var state = activeChallenges.get(player);
        if (state == null) return List.of();
        return getAllObjectives(state);
    }

    /**
     * Check if the player's active challenge is complete.
     */
    public static boolean isChallengeComplete(@NotNull ServerPlayer player) {
        var state = activeChallenges.get(player);
        if (state == null) return false;
        return getAllObjectives(state).stream().allMatch(obj -> obj.isComplete(player));
    }

    // -- Persistence ---------------------------------------------------------

    /**
     * Save challenge state to the player's persistent data.
     */
    public static void saveToPlayer(@NotNull ServerPlayer player) {
        var tag = player.getPersistentData();
        var data = new CompoundTag();
        var state = activeChallenges.get(player);
        if (state == null) return;
        data.putString("challenge_name", state.challenge.name());
        data.putLong("start_tick", state.startTick);
        player.getPersistentData().put("corelib_challenge", data);
    }

    /**
     * Load challenge state from persistent data.
     */
    public static void loadFromPlayer(@NotNull ServerPlayer player) {
        var data = player.getPersistentData().getCompound("corelib_challenge");
        if (data.isEmpty()) return;
        // Restore is handled at a higher level by re-creating the challenge from the registry
    }

    // -- Internal ------------------------------------------------------------

    private static List<Objective> getAllObjectives(ChallengeState state) {
        var result = new ArrayList<>(state.challenge.objectives());
        result.addAll(state.customObjectives);
        return Collections.unmodifiableList(result);
    }

    private static final class ChallengeState {
        final Challenge challenge;
        final List<Objective> customObjectives = new ArrayList<>();
        final long startTick;

        ChallengeState(Challenge challenge) {
            this.challenge = challenge;
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            this.startTick = server != null ? server.overworld().getGameTime() : 0;
        }
    }

    // -- Event handler -------------------------------------------------------

    private static final class ObjectiveHandler {
        private ObjectiveHandler() {
        }

        @SubscribeEvent
        static void onTick(@NotNull ServerTickEvent.Post event) {
            // Check for completed challenges every 20 ticks (1 second)
            if (event.getServer().getTickCount() % 20 != 0) return;

            var completed = new ArrayList<Map.Entry<ServerPlayer, ChallengeState>>();
            for (var entry : activeChallenges.entrySet()) {
                if (getAllObjectives(entry.getValue()).stream().allMatch(obj -> obj.isComplete(entry.getKey()))) {
                    completed.add(entry);
                }
            }

            for (var entry : completed) {
                var player = entry.getKey();
                var state = entry.getValue();
                var server = player.serverLevel().getServer();
                MessageHelper.broadcastChat(
                        MessageHelper.aqua("Challenge completed: ").append(
                                MessageHelper.gold(state.challenge.name())),
                        server);
                activeChallenges.remove(player);
                SurvivalObjective.stop(player);
            }
        }
    }
}
