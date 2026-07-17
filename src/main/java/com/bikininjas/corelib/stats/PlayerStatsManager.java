package com.bikininjas.corelib.stats;

import com.bikininjas.corelib.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks player statistics automatically: deaths, kills, blocks broken, and crafts.
 * <p>
 * Thread-safe. All methods static. Registers event handlers on the NeoForge event bus.
 */
public final class PlayerStatsManager {

    private static final Map<UUID, PlayerStats> statsMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSyncTick = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.register(StatsHandler.class);
    }

    private PlayerStatsManager() {
    }

    /**
     * Get the full stats for a player.
     */
    public static @NotNull PlayerStats getStats(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        return statsMap.getOrDefault(player.getUUID(), PlayerStats.EMPTY);
    }

    /**
     * Get stats by player UUID.
     */
    public static @NotNull PlayerStats getStats(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return statsMap.getOrDefault(uuid, PlayerStats.EMPTY);
    }

    /**
     * Get the death count for a player.
     */
    public static int getDeaths(@NotNull ServerPlayer player) {
        return getStats(player).deaths();
    }

    /**
     * Get the kill count for a player.
     */
    public static int getKills(@NotNull ServerPlayer player) {
        return getStats(player).kills();
    }

    /**
     * Get the blocks broken count for a player.
     */
    public static int getBlocksBroken(@NotNull ServerPlayer player) {
        return getStats(player).blocksBroken();
    }

    /**
     * Get the craft count for a player.
     */
    public static int getCrafts(@NotNull ServerPlayer player) {
        return getStats(player).crafts();
    }

    /**
     * Reset all stats for a player to zero.
     */
    public static void resetStats(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        statsMap.put(player.getUUID(), PlayerStats.EMPTY);
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     */
    public static void init() {
        // static initializer already ran; explicit call for module loading
    }

    // -- Persistence ---------------------------------------------------------

    /**
     * Save player stats to persistent NBT data.
     */
    private static void savePlayerStats(@NotNull ServerPlayer player) {
        var stats = getStats(player);
        var tag = new CompoundTag();
        tag.putInt("deaths", stats.deaths());
        tag.putInt("kills", stats.kills());
        tag.putInt("blocksBroken", stats.blocksBroken());
        tag.putInt("crafts", stats.crafts());
        player.getPersistentData().put("corelib_stats", tag);
    }

    /**
     * Load player stats from persistent NBT data.
     */
    private static void loadPlayerStats(@NotNull ServerPlayer player) {
        var tag = player.getPersistentData().getCompound("corelib_stats");
        if (!tag.isEmpty()) {
            var loaded = new PlayerStats(
                tag.getInt("deaths"), tag.getInt("kills"),
                tag.getInt("blocksBroken"), tag.getInt("crafts")
            );
            statsMap.put(player.getUUID(), loaded);
        }
    }

    // -- Internal mutation ---------------------------------------------------

    private static PlayerStats mutate(ServerPlayer player, java.util.function.UnaryOperator<PlayerStats> mutator) {
        var uuid = player.getUUID();
        var current = statsMap.getOrDefault(uuid, PlayerStats.EMPTY);
        var updated = mutator.apply(current);
        statsMap.put(uuid, updated);
        return updated;
    }

    /**
     * Check if enough ticks have passed since the last sync (minimum 20 ticks / 1 second).
     */
    private static boolean shouldSync(@NotNull ServerPlayer player) {
        var currentTick = player.serverLevel().getGameTime();
        var lastSync = lastSyncTick.getOrDefault(player.getUUID(), 0L);
        if (currentTick - lastSync >= 20) {
            lastSyncTick.put(player.getUUID(), currentTick);
            return true;
        }
        return false;
    }

    // -- Event handler -------------------------------------------------------

    private static final class StatsHandler {
        private StatsHandler() {
        }

        @SubscribeEvent
        static void onPlayerLogin(@NotNull PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                loadPlayerStats(player);
                NetworkHandler.sendStatsSync(player);
            }
        }

        @SubscribeEvent
        static void onPlayerLogout(@NotNull PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                savePlayerStats(player);
                statsMap.remove(player.getUUID());
            }
        }

        @SubscribeEvent
        static void onDeath(@NotNull LivingDeathEvent event) {
            var victim = event.getEntity() instanceof ServerPlayer v ? v : null;
            var killer = event.getSource().getEntity() instanceof ServerPlayer k ? k : null;
            if (victim == null && killer == null) return;

            if (victim != null && killer != null && victim.getUUID().equals(killer.getUUID())) {
                mutate(victim, s -> new PlayerStats(s.deaths() + 1, s.kills() + 1, s.blocksBroken(), s.crafts()));
                if (shouldSync(victim)) NetworkHandler.sendStatsSync(victim);
            } else {
                if (victim != null) {
                    mutate(victim, s -> new PlayerStats(s.deaths() + 1, s.kills(), s.blocksBroken(), s.crafts()));
                    if (shouldSync(victim)) NetworkHandler.sendStatsSync(victim);
                }
                if (killer != null) {
                    mutate(killer, s -> new PlayerStats(s.deaths(), s.kills() + 1, s.blocksBroken(), s.crafts()));
                    if (shouldSync(killer)) NetworkHandler.sendStatsSync(killer);
                }
            }
        }

        @SubscribeEvent
        static void onBlockBreak(@NotNull BlockEvent.BreakEvent event) {
            if (event.getPlayer() instanceof ServerPlayer player) {
                mutate(player, s -> new PlayerStats(s.deaths(), s.kills(), s.blocksBroken() + 1, s.crafts()));
                if (shouldSync(player)) NetworkHandler.sendStatsSync(player);
            }
        }

        @SubscribeEvent
        static void onCraft(@NotNull PlayerEvent.ItemCraftedEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                mutate(player, s -> new PlayerStats(s.deaths(), s.kills(), s.blocksBroken(), s.crafts() + 1));
                if (shouldSync(player)) NetworkHandler.sendStatsSync(player);
            }
        }
    }
}
