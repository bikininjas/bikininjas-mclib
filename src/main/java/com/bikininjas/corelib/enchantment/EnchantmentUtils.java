package com.bikininjas.corelib.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

/**
 * Static utilities for applying high-level (above vanilla max) enchantments.
 * <p>
 * Vanilla Minecraft caps enchantment levels at the enchantment's defined
 * {@link Enchantment#getMaxLevel()}. These helpers allow enchanting up to
 * level {@link #MAX_LEVEL} by relying on
 * {@link ItemStack#enchant(Holder, int)}, which bypasses the max-level
 * validation performed by the normal enchantment application path.
 */
public final class EnchantmentUtils {

    /** Hard cap for any enchantment level handled by this library. */
    public static final int MAX_LEVEL = 100;

    private EnchantmentUtils() {
        // Utility class — do not instantiate.
    }

    /**
     * Apply a single enchantment at any level, bypassing the vanilla max-level check.
     *
     * @param stack       the item to enchant (mutated and returned for chaining)
     * @param enchantment the enchantment holder to apply
     * @param level       the desired level (clamped into {@code [1, MAX_LEVEL]})
     * @return the same (mutated) {@link ItemStack}
     */
    public static ItemStack applyEnchantment(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        stack.enchant(enchantment, clamp(level));
        return stack;
    }

    /**
     * Apply multiple enchantments at once, each bypassing the vanilla max-level check.
     *
     * @param stack        the item to enchant (mutated and returned for chaining)
     * @param enchantments map of enchantment holder to desired level
     * @return the same (mutated) {@link ItemStack}
     */
    public static ItemStack applyEnchantments(ItemStack stack, Map<Holder<Enchantment>, Integer> enchantments) {
        for (var entry : enchantments.entrySet()) {
            stack.enchant(entry.getKey(), clamp(entry.getValue()));
        }
        return stack;
    }

    /**
     * Effective maximum level for a given tool: the enchantment's vanilla max level
     * multiplied by 3, capped at {@link #MAX_LEVEL}.
     *
     * @param enchantment the enchantment holder
     * @param tool        the tool/item being enchanted (unused for the cap math)
     * @return the effective maximum level, never above {@link #MAX_LEVEL}
     */
    public static int getMaxLevelForTool(Holder<Enchantment> enchantment, ItemStack tool) {
        return Math.min(enchantment.value().getMaxLevel() * 3, MAX_LEVEL);
    }

    /**
     * Whether an enchantment may be applied at the given level.
     * <p>
     * Overrides the vanilla check: always allowed as long as {@code level} is within
     * the library cap {@link #MAX_LEVEL}.
     *
     * @param enchantment the enchantment holder
     * @param level       the desired level
     * @return {@code true} if {@code level} is within the library cap
     */
    public static boolean canEnchantAtLevel(Holder<Enchantment> enchantment, int level) {
        return level <= MAX_LEVEL;
    }

    /**
     * Clamp a requested level into the valid range {@code [1, MAX_LEVEL]}.
     */
    private static int clamp(int level) {
        return Math.max(1, Math.min(level, MAX_LEVEL));
    }
}
