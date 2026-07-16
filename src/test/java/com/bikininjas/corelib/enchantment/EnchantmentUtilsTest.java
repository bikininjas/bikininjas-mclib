package com.bikininjas.corelib.enchantment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentUtilsTest {

    @Test
    void canEnchantAtLevelReturnsTrueForZero() {
        assertTrue(EnchantmentUtils.canEnchantAtLevel(0));
    }

    @Test
    void canEnchantAtLevelReturnsTrueForMaxLevel() {
        assertTrue(EnchantmentUtils.canEnchantAtLevel(EnchantmentUtils.MAX_LEVEL));
    }

    @Test
    void canEnchantAtLevelReturnsTrueForMidRange() {
        assertTrue(EnchantmentUtils.canEnchantAtLevel(50));
    }

    @Test
    void canEnchantAtLevelReturnsFalseForNegative() {
        assertFalse(EnchantmentUtils.canEnchantAtLevel(-1));
    }

    @Test
    void canEnchantAtLevelReturnsFalseForAboveMax() {
        assertFalse(EnchantmentUtils.canEnchantAtLevel(EnchantmentUtils.MAX_LEVEL + 1));
    }
}
