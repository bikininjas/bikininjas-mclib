package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.enchantment.EnchantmentUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link EnchantmentUtils}.
 * <p>
 * These test only the pure logic that doesn't require a live Minecraft server:
 * structural guarantees (final class, private constructor), constant values,
 * and the boundary checks in {@link EnchantmentUtils#canEnchantAtLevel}.
 * <p>
 * Tests that require a live {@code Enchantment} holder (applying enchantments
 * to items, querying max level) are written as NeoForge {@code GameTest}s
 * instead, because the enchantment registry is data-driven and only available
 * inside a running game in Minecraft 1.21.1+.
 */
class EnchantmentUtilsTests {

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(EnchantmentUtils.class.getModifiers()),
                "EnchantmentUtils must be a final utility class");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<EnchantmentUtils> ctor = EnchantmentUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "EnchantmentUtils constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(),
                "Private constructor should be invokable (no side effects)");
    }

    @Test
    void maxLevelConstantIs100() {
        assertEquals(100, EnchantmentUtils.MAX_LEVEL);
    }

    @Test
    void canEnchantAtLevelRespectsCap() {
        // canEnchantAtLevel only checks level <= MAX_LEVEL — it doesn't
        // dereference the Holder, so null is safe here.
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 1));
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 100));
        assertFalse(EnchantmentUtils.canEnchantAtLevel(null, 101));
        assertFalse(EnchantmentUtils.canEnchantAtLevel(null, 1000));
    }

    @Test
    void canEnchantAtLevelAcceptsBoundaryValues() {
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 0));   // below min
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 1));   // min
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 50));  // mid
        assertTrue(EnchantmentUtils.canEnchantAtLevel(null, 100)); // max
        assertFalse(EnchantmentUtils.canEnchantAtLevel(null, 101));// above max
    }
}
