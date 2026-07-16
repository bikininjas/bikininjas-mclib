package com.bikininjas.corelib.client;

import com.bikininjas.corelib.network.StatsSyncPayload;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side singleton cache of the latest synced stats.
 * <p>
 * Updated by {@link StatsSyncPayload} via the network handler.
 * The entire state is swapped atomically via an {@link AtomicReference},
 * so the render thread always sees a consistent snapshot written
 * by the Netty thread (no partial-update race).
 */
public final class StatsClientData {

    private static final AtomicReference<State> STATE =
            new AtomicReference<>(new State(false, Set.of(), 0, 0, 0, 0));

    private StatsClientData() {}

    private record State(
            boolean visible,
            Set<String> visibleFields,
            int deaths, int kills,
            int blocksBroken, int crafts
    ) {}

    /**
     * Atomically replace all cached state with the values from a payload.
     */
    public static void update(StatsSyncPayload payload) {
        STATE.set(new State(
                payload.visible(),
                payload.fields(),
                payload.deaths(),
                payload.kills(),
                payload.blocksBroken(),
                payload.crafts()
        ));
    }

    // ──────────────────────────────────────────────
    //  Queries
    // ──────────────────────────────────────────────

    public static boolean isVisible() {
        return STATE.get().visible();
    }

    public static Set<String> getVisibleFields() {
        return STATE.get().visibleFields();
    }

    public static int getDeaths() {
        return STATE.get().deaths();
    }

    public static int getKills() {
        return STATE.get().kills();
    }

    public static int getBlocksBroken() {
        return STATE.get().blocksBroken();
    }

    public static int getCrafts() {
        return STATE.get().crafts();
    }
}
