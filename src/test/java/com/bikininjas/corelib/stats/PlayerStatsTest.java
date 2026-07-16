package com.bikininjas.corelib.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStatsTest {

    @Test
    void emptyConstantHasAllZeros() {
        assertEquals(0, PlayerStats.EMPTY.deaths());
        assertEquals(0, PlayerStats.EMPTY.kills());
        assertEquals(0, PlayerStats.EMPTY.blocksBroken());
        assertEquals(0, PlayerStats.EMPTY.crafts());
    }

    @Test
    void recordStoresAllFields() {
        var stats = new PlayerStats(5, 12, 340, 27);
        assertEquals(5, stats.deaths());
        assertEquals(12, stats.kills());
        assertEquals(340, stats.blocksBroken());
        assertEquals(27, stats.crafts());
    }

    @Test
    void recordEquality() {
        var a = new PlayerStats(1, 2, 3, 4);
        var b = new PlayerStats(1, 2, 3, 4);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void recordInequalityByField() {
        var a = new PlayerStats(1, 2, 3, 4);
        var b = new PlayerStats(1, 2, 3, 5);
        assertNotEquals(a, b);
    }
}
