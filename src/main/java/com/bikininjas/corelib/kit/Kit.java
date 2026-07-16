package com.bikininjas.corelib.kit;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Immutable definition of a player kit: a named bundle of items, armor, an
 * offhand item and status effects that can be handed to a player via
 * {@link KitManager#give(net.minecraft.server.level.ServerPlayer, String)}.
 * <p>
 * Instances are plain records and therefore immutable and thread-safe to read
 * once published into the {@link KitManager} registry.
 *
 * @param name    the unique registry key / display name of the kit (non-blank)
 * @param items   the main-inventory items (may be empty, never {@code null})
 * @param armor   the four armor pieces, indexed as {@code [boots, leggings, chestplate, helmet]}
 *                (must be length 4, never {@code null})
 * @param offhand the item placed in the player's offhand (may be {@link ItemStack#EMPTY})
 * @param effects the status effects applied on give (may be empty, never {@code null})
 */
public record Kit(
        String name,
        ItemStack[] items,
        ItemStack[] armor,
        ItemStack offhand,
        Collection<MobEffectInstance> effects
) {
}
