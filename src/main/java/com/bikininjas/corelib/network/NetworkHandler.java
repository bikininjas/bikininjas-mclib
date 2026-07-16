package com.bikininjas.corelib.network;

import com.bikininjas.corelib.CoreLib;
import com.bikininjas.corelib.client.StatsClientData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Registers all core-lib network payloads on the mod event bus.
 * <p>
 * Call {@link #register(IEventBus)} from {@link CoreLib#CoreLib(IEventBus)}.
 */
public final class NetworkHandler {

    private NetworkHandler() {}

    /**
     * Register network payload handlers on the mod event bus.
     *
     * @param modBus the mod event bus from {@link CoreLib#CoreLib(IEventBus)}.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar("1.0");
            registrar.playToClient(
                    StatsSyncPayload.TYPE,
                    StatsSyncPayload.STREAM_CODEC,
                    (payload, context) ->
                            StatsClientData.update(payload)
            );
        });
    }
}
