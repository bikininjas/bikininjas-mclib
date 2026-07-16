package com.bikininjas.corelib.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Utility for saving, restoring, and clearing a player's full state.
 * <p>
 * All methods are static. No event bus registration.
 */
public final class PlayerStateManager {

    private PlayerStateManager() {
    }

    /**
     * Capture the current state of a player.
     *
     * @return an immutable snapshot
     */
    public static @NotNull PlayerState save(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        return PlayerState.capture(player);
    }

    /**
     * Restore a previously saved state onto a player.
     * <p>
     * Clears the player's current inventory first, then applies the saved state.
     */
    public static void load(@NotNull ServerPlayer player, @NotNull PlayerState state) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(state, "state must not be null");

        // Clear current inventory
        player.getInventory().clearContent();

        // Restore main inventory
        var inv = player.getInventory();
        for (int i = 0; i < state.mainInventory().size(); i++) {
            inv.setItem(i, state.mainInventory().get(i).copy());
        }

        // Restore armor (armor slots are indices 36-39 in the player inventory)
        for (int i = 0; i < state.armorInventory().size(); i++) {
            inv.setItem(36 + i, state.armorInventory().get(i).copy());
        }

        // Restore offhand (offhand slot is index 40)
        inv.setItem(40, state.offhand().copy());

        // Restore health, food, XP
        player.setHealth(state.health());
        var foodData = player.getFoodData();
        // Use resetFood to set all three at once
        foodData.setFoodLevel(state.food());
        foodData.setSaturation(state.saturation());
        player.setExperienceLevels(state.xpLevel());
        player.experienceProgress = state.xpProgress();

        // Clear and re-apply effects
        player.removeAllEffects();
        for (var effect : state.effects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
    }

    /**
     * Clear a player's inventory, stats, and effects (reset to default state).
     */
    public static void clear(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        var foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0f);
        player.setExperienceLevels(0);
        player.experienceProgress = 0.0f;
        player.removeAllEffects();
    }
}
