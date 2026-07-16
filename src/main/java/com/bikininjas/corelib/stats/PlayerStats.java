package com.bikininjas.corelib.stats;

/**
 * Immutable player statistics snapshot.
 */
public record PlayerStats(
        int deaths,
        int kills,
        int blocksBroken,
        int crafts
) {
    /** Empty/zero stats constant. */
    public static final PlayerStats EMPTY = new PlayerStats(0, 0, 0, 0);
}
