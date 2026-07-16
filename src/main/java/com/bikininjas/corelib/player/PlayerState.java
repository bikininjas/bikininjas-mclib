package com.bikininjas.corelib.player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;

import java.util.Collection;

/**
 * Immutable snapshot of a {@code ServerPlayer}'s full gameplay state.
 * <p>
 * Captures everything needed to faithfully save and later restore a player:
 * the three inventory compartments (main, armor, offhand), vital statistics
 * (health, food, saturation), experience, active status effects, and the
 * current game mode.
 * <p>
 * Instances are produced by {@link PlayerStateManager#save} and consumed by
 * {@link PlayerStateManager#load}. All inventory arrays are deep copies taken
 * at capture time, so mutating the live player afterwards never alters a
 * previously captured snapshot.
 *
 * @param mainInventory   the 36-slot main inventory (index 0..35)
 * @param armorInventory  the 4-slot armor inventory (index 0..3)
 * @param offhand         the single offhand item
 * @param health          current health points
 * @param food            current food level (0..20)
 * @param saturation      current saturation level
 * @param xpLevel         current experience level
 * @param xpProgress      experience progress within the current level (0.0..1.0)
 * @param effects         active status effects at capture time
 * @param gameType        the game mode the player was in
 */
public record PlayerState(
        ItemStack[] mainInventory,
        ItemStack[] armorInventory,
        ItemStack offhand,
        float health,
        int food,
        float saturation,
        int xpLevel,
        float xpProgress,
        Collection<MobEffectInstance> effects,
        GameType gameType
) {
}
