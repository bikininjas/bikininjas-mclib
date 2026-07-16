package com.bikininjas.corelib.stats;

import com.bikininjas.corelib.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

    // -- Internal mutation ---------------------------------------------------

    private static PlayerStats mutate(ServerPlayer player, java.util.function.UnaryOperator<PlayerStats> mutator) {
        var uuid = player.getUUID();
        var current = statsMap.getOrDefault(uuid, PlayerStats.EMPTY);
        var updated = mutator.apply(current);
        statsMap.put(uuid, updated);
        return updated;
    }

    // -- Event handler -------------------------------------------------------

    private static final class StatsHandler {
        private StatsHandler() {
        }

        @SubscribeEvent
        static void onPlayerLogin(@NotNull PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                NetworkHandler.sendStatsSync(player);
            }
        }

        @SubscribeEvent
        static void onDeath(@NotNull LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer victim) {
                mutate(victim, s -> new PlayerStats(s.deaths() + 1, s.kills(), s.blocksBroken(), s.crafts()));
            }
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                mutate(killer, s -> new PlayerStats(s.deaths(), s.kills() + 1, s.blocksBroken(), s.crafts()));
            }
        }

        @SubscribeEvent
        static void onBlockBreak(@NotNull BlockEvent.BreakEvent event) {
            if (event.getPlayer() instanceof ServerPlayer player) {
                mutate(player, s -> new PlayerStats(s.deaths(), s.kills(), s.blocksBroken() + 1, s.crafts()));
            }
        }

        @SubscribeEvent
        static void onCraft(@NotNull PlayerEvent.ItemCraftedEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                mutate(player, s -> new PlayerStats(s.deaths(), s.kills(), s.blocksBroken(), s.crafts() + 1));
            }
        }
    }
}
