package com.bikininjas.corelib.cooldown;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe centralized cooldown tracker.
 * <p>
 * Each cooldown is keyed by a string identifier (e.g. "mystery_box", "void_pearl")
 * and mapped per-player via {@link UUID}. The value stored is the server tick
 * at which the cooldown expires.
 * <p>
 * All methods are static. No singleton.
 */
public final class CooldownManager {

    private static final ModLogger LOGGER = LogManager.getLogger("core_lib", CooldownManager.class);

    /** cooldownKey → playerUUID → expiryTick */
    private static final Map<String, Map<UUID, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    /** Periodic cleanup threshold: every N ticks, expired entries are purged. */
    private static final int CLEANUP_INTERVAL_TICKS = 100;

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private CooldownManager() {
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     * Triggers static initializer (event bus registration).
     */
    public static void init() {
    }

    // -- Public API ----------------------------------------------------------

    /**
     * Check whether the given player is still on cooldown for the given key.
     * <p>
     * <b>Deprecated</b> — without a ServerLevel the expiry tick cannot be compared
     * to the current server tick. This method only checks whether an entry exists,
     * which returns true for expired cooldowns awaiting periodic cleanup.
     * Prefer {@link #isOnCooldown(ServerLevel, UUID, String)}.
     *
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     * @return true if an entry exists (may be stale)
     * @deprecated Use {@link #isOnCooldown(ServerLevel, UUID, String)} for accurate results
     */
    @Deprecated
    public static boolean isOnCooldown(@NotNull UUID playerId, @NotNull String key) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        var playerMap = COOLDOWNS.get(key);
        if (playerMap == null) {
            return false;
        }
        Long expiry = playerMap.get(playerId);
        return expiry != null;
    }

    /**
     * Check whether the given player is still on cooldown, comparing the
     * stored expiry tick against the current server tick.
     *
     * @param level    the current server level (for tick access)
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     * @return true if the cooldown is still active
     */
    public static boolean isOnCooldown(@NotNull net.minecraft.server.level.ServerLevel level,
                                       @NotNull UUID playerId,
                                       @NotNull String key) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        var playerMap = COOLDOWNS.get(key);
        if (playerMap == null) {
            return false;
        }
        Long expiry = playerMap.get(playerId);
        if (expiry == null) {
            return false;
        }
        return expiry > level.getServer().getTickCount();
    }

    /**
     * Check if the given player is on cooldown for the given key.
     * Without a {@link net.minecraft.server.level.ServerLevel}, only a binary
     * answer is available. For exact remaining ticks, use
     * {@link #getRemainingTicks(net.minecraft.server.level.ServerLevel, UUID, String)}.
     *
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     * @return 1 if still on cooldown, 0 otherwise
     * @deprecated Use {@link #getRemainingTicks(ServerLevel, UUID, String)} for accurate results
     */
    @Deprecated
    public static long getRemainingTicks(@NotNull UUID playerId, @NotNull String key) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        var playerMap = COOLDOWNS.get(key);
        if (playerMap == null) {
            return 0;
        }
        Long expiry = playerMap.get(playerId);
        return (expiry != null) ? 1 : 0;
    }

    /**
     * Get the remaining cooldown ticks with a level reference for exact calculation.
     *
     * @param level    the server level providing the current tick
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     * @return remaining ticks, 0 if expired or not on cooldown
     */
    public static long getRemainingTicks(@NotNull net.minecraft.server.level.ServerLevel level,
                                          @NotNull UUID playerId,
                                          @NotNull String key) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        var playerMap = COOLDOWNS.get(key);
        if (playerMap == null) {
            return 0;
        }
        Long expiry = playerMap.get(playerId);
        if (expiry == null) {
            return 0;
        }
        long remaining = expiry - level.getServer().getTickCount();
        return Math.max(0, remaining);
    }

    /**
     * Set a cooldown for the given player.
     *
     * @param level    the server level providing the current tick
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     * @param ticks    cooldown duration in ticks
     */
    public static void setCooldown(@NotNull net.minecraft.server.level.ServerLevel level,
                                    @NotNull UUID playerId,
                                    @NotNull String key,
                                    long ticks) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        long expiry = level.getServer().getTickCount() + ticks;
        COOLDOWNS.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                 .put(playerId, expiry);
        LOGGER.debug("Cooldown '{}' set for player {}: {} ticks", key, playerId, ticks);
    }

    /**
     * Remove a specific cooldown for the given player.
     *
     * @param playerId the player's UUID
     * @param key      cooldown identifier
     */
    public static void clearCooldown(@NotNull UUID playerId, @NotNull String key) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(key, "key must not be null");

        var playerMap = COOLDOWNS.get(key);
        if (playerMap != null) {
            playerMap.remove(playerId);
        }
    }

    /**
     * Remove all cooldowns for a specific player across all keys.
     *
     * @param playerId the player's UUID
     */
    public static void clearAllCooldowns(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");

        for (var playerMap : COOLDOWNS.values()) {
            playerMap.remove(playerId);
        }
    }

    /**
     * Remove all cooldowns for all players (e.g. on world unload).
     */
    public static void clearAll() {
        COOLDOWNS.clear();
    }

    // -- Event handler -------------------------------------------------------

    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        static void onServerTick(@NotNull ServerTickEvent.Post event) {
            var server = event.getServer();
            if (server.getTickCount() % CLEANUP_INTERVAL_TICKS != 0) {
                return;
            }
            long now = server.getTickCount();
            for (var playerMap : COOLDOWNS.values()) {
                var it = playerMap.entrySet().iterator();
                while (it.hasNext()) {
                    var entry = it.next();
                    if (entry.getValue() <= now) {
                        it.remove();
                    }
                }
            }
        }
    }
}
