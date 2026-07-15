package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.enchantment.EnchantmentUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit5 unit tests for {@link EnchantmentUtils}.
 * <p>
 * Run with {@code ./gradlew test} (NeoForge {@code unitTest} block, which
 * bootstraps a Minecraft environment so registry-held objects are available).
 */
class EnchantmentUtilsTests {

    private static final Holder<Enchantment> SHARPNESS = RegistryAccess
            .fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SHARPNESS);

    @Test
    void canEnchantAtLevelRespectsCap() {
        assertTrue(EnchantmentUtils.canEnchantAtLevel(SHARPNESS, 1));
        assertTrue(EnchantmentUtils.canEnchantAtLevel(SHARPNESS, 100));
        assertFalse(EnchantmentUtils.canEnchantAtLevel(SHARPNESS, 101));
        assertFalse(EnchantmentUtils.canEnchantAtLevel(SHARPNESS, 1000));
    }

    @Test
    void getMaxLevelForToolTriplesVanillaCap() {
        // Sharpness vanilla max level is 5 -> 5 * 3 = 15
        int max = EnchantmentUtils.getMaxLevelForTool(SHARPNESS, new ItemStack(Items.DIAMOND_SWORD));
        assertEquals(15, max);
        assertTrue(max <= EnchantmentUtils.MAX_LEVEL);
    }

    @Test
    void applyEnchantmentBypassesVanillaMax() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentUtils.applyEnchantment(sword, SHARPNESS, 50);
        assertEquals(50, sword.getEnchantmentLevel(SHARPNESS));
    }

    @Test
    void applyEnchantmentClampsToCap() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentUtils.applyEnchantment(sword, SHARPNESS, 500);
        assertEquals(EnchantmentUtils.MAX_LEVEL, sword.getEnchantmentLevel(SHARPNESS));
    }

    @Test
    void applyEnchantmentsAppliesAll() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentUtils.applyEnchantments(sword, Map.of(SHARPNESS, 20));
        assertEquals(20, sword.getEnchantmentLevel(SHARPNESS));
    }
}
