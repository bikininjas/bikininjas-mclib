package com.bikininjas.corelib.network;

import com.bikininjas.corelib.stats.PlayerStatsManager;
import com.bikininjas.corelib.stats.StatsDisplayPrefs;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all core-lib network payloads.
 * <p>
 * Must be called from the owning mod's constructor with the mod event bus.
 */
public final class NetworkHandler {

    private NetworkHandler() {
    }

    /**
     * Register all payloads on the given mod bus.
     * <p>
     * Usage from {@code @Mod} constructor:
     * <pre>{@code
     * NetworkHandler.register(modBus);
     * }</pre>
     */
    public static void register(@NotNull IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("core_lib");
            registrar.playToClient(
                    StatsSyncPayload.TYPE,
                    StatsSyncPayload.STREAM_CODEC,
                    (payload, context) -> {
                        StatsClientData.update(payload);
                    }
            );
        });
    }

    /**
     * Build and send a stats + prefs sync payload to a player.
     */
    public static void sendStatsSync(@NotNull ServerPlayer player) {
        var stats = PlayerStatsManager.getStats(player);
        var enabled = StatsDisplayPrefs.isEnabled(player);
        var bitmask = StatsDisplayPrefs.toBitmask(StatsDisplayPrefs.getVisibleFields(player));
        var payload = new StatsSyncPayload(stats, enabled, bitmask);
        PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * Thread-safe client-side cache for the latest received stats.
     */
    public static final class StatsClientData {
        private static volatile StatsSyncPayload latest = new StatsSyncPayload(0, 0, 0, 0, true, StatsDisplayPrefs.VISIBLE_ALL);

        public static void update(@NotNull StatsSyncPayload payload) {
            latest = payload;
        }

        public static @NotNull StatsSyncPayload getLatest() {
            return latest;
        }

        public static boolean isOverlayEnabled() {
            return latest.overlayEnabled();
        }

        public static boolean isFieldVisible(int bit) {
            return (latest.visibleFields() & bit) != 0;
        }

        private StatsClientData() {
        }
    }
}
