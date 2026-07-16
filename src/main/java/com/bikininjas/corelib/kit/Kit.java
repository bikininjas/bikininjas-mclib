package com.bikininjas.corelib.kit;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable kit definition: a named collection of items, armor, offhand, and effects.
 */
public record Kit(
        @NotNull String name,
        @NotNull List<ItemStack> items,
        @NotNull List<ItemStack> armor,
        @NotNull ItemStack offhand,
        @NotNull List<MobEffectInstance> effects
) {

    public Kit {
        items = items == null ? List.of() : List.copyOf(items);
        armor = armor == null ? List.of() : List.copyOf(armor);
        offhand = offhand == null ? ItemStack.EMPTY : offhand;
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /**
     * Create a simple kit with just items (no armor, offhand, or effects).
     */
    public static @NotNull Kit of(@NotNull String name, @NotNull ItemStack... items) {
        return new Kit(name, List.of(items), List.of(), ItemStack.EMPTY, List.of());
    }
}
