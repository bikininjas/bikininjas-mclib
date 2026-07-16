package com.bikininjas.corelib.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Static utility for capturing and restoring a {@link ServerPlayer}'s full
 * gameplay state.
 * <p>
 * The manager is fully stateless: it holds no fields, subscribes to no events,
 * and registers nothing. All operations are pure functions over a player and a
 * {@link PlayerState} snapshot.
 * <p>
 * Inventory is captured compartment-by-compartment using the live
 * {@link Inventory} fields ({@code items} for the 36-slot main inventory,
 * {@code armor} for the 4-slot armor, and {@code offhand} for the single
 * offhand slot). Every stack is copied via {@link ItemStack#copy()} so the
 * snapshot is independent of the live player. Restoration writes each
 * compartment back through the appropriate setter and resets vitals,
 * experience, effects, and game mode.
 */
public final class PlayerStateManager {

    /** Size of the main inventory compartment. */
    private static final int MAIN_SIZE = 36;
    /** Size of the armor inventory compartment. */
    private static final int ARMOR_SIZE = 4;
    /** Default health restored by {@link #clear}. */
    private static final float DEFAULT_HEALTH = 20.0F;
    /** Default food level restored by {@link #clear}. */
    private static final int DEFAULT_FOOD = 20;
    /** Default saturation restored by {@link #clear}. */
    private static final float DEFAULT_SATURATION = 5.0F;

    private PlayerStateManager() {
        // Utility class — do not instantiate.
    }

    /**
     * Capture the full current state of a player into an immutable snapshot.
     * <p>
     * Every inventory stack is deep-copied, so the returned {@link PlayerState}
     * is safe to keep around and will not change when the live player is later
     * mutated.
     *
     * @param player the player to snapshot (must not be {@code null})
     * @return a new {@link PlayerState} holding copies of all captured fields
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public static PlayerState save(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");

        Inventory inventory = player.getInventory();
        ItemStack[] main = copyArray(inventory.items, MAIN_SIZE);
        ItemStack[] armor = copyArray(inventory.armor, ARMOR_SIZE);
        ItemStack offhand = inventory.offhand.isEmpty()
                ? ItemStack.EMPTY
                : inventory.offhand.get(0).copy();

        Collection<MobEffectInstance> effects = player.getActiveEffects().stream()
                .map(MobEffectInstance::new)
                .toList();

        return new PlayerState(
                main,
                armor,
                offhand,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.experienceLevel,
                player.experienceProgress,
                effects,
                player.gameMode.getGameModeForPlayer()
        );
    }

    /**
     * Restore a previously captured {@link PlayerState} onto a player.
     * <p>
     * The main inventory, armor, and offhand are written through
     * {@link Inventory#setItem(int, ItemStack)} using the compartment offsets
     * (main 0..35, armor 36..39, offhand 40). Vitals, experience, effects, and
     * game mode are restored via the player's own setters. The player's
     * inventory is marked dirty so the client receives the update.
     *
     * @param player the player to restore onto (must not be {@code null})
     * @param state  the snapshot to apply (must not be {@code null})
     * @throws NullPointerException if either argument is {@code null}
     */
    public static void load(ServerPlayer player, PlayerState state) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(state, "state must not be null");

        Inventory inventory = player.getInventory();

        // Inventory.set(compartment) indices: main 0..35, armor 36..39, offhand 40.
        for (int i = 0; i < MAIN_SIZE; i++) {
            inventory.setItem(i, safeCopy(state.mainInventory(), i));
        }
        for (int i = 0; i < ARMOR_SIZE; i++) {
            inventory.setItem(i + MAIN_SIZE, safeCopy(state.armorInventory(), i));
        }
        inventory.setItem(Inventory.SLOT_OFFHAND, copyOne(state.offhand()));

        player.setHealth(state.health());
        player.getFoodData().setFoodLevel(state.food());
        player.getFoodData().setSaturation(state.saturation());
        player.setExperienceLevels(state.xpLevel());
        player.experienceProgress = state.xpProgress();

        player.removeAllEffects();
        for (MobEffectInstance effect : state.effects()) {
            player.addEffect(new MobEffectInstance(effect));
        }

        player.setGameMode(state.gameType());
        inventory.setChanged();
    }

    /**
     * Reset a player to a clean, default survival state.
     * <p>
     * Clears the entire inventory and restores: full health (20),
     * full food (20), default saturation (5), zero experience, no active
     * effects, and {@link GameType#SURVIVAL} game mode.
     *
     * @param player the player to reset (must not be {@code null})
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public static void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");

        player.getInventory().clearContent();
        player.setHealth(DEFAULT_HEALTH);
        player.getFoodData().setFoodLevel(DEFAULT_FOOD);
        player.getFoodData().setSaturation(DEFAULT_SATURATION);
        player.setExperienceLevels(0);
        player.experienceProgress = 0.0F;
        player.removeAllEffects();
        player.setGameMode(GameType.SURVIVAL);
    }

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    /**
     * Copy an inventory compartment into a fixed-size array, deep-copying each
     * non-empty stack. Missing or out-of-bounds entries become
     * {@link ItemStack#EMPTY}.
     */
    private static ItemStack[] copyArray(Collection<ItemStack> source, int size) {
        ItemStack[] result = new ItemStack[size];
        Arrays.fill(result, ItemStack.EMPTY);
        int i = 0;
        for (ItemStack stack : source) {
            if (i >= size) break;
            result[i++] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        return result;
    }

    /**
     * Return a deep copy of the stack at {@code index}, or {@link ItemStack#EMPTY}
     * if the index is out of bounds or the slot is empty.
     */
    private static ItemStack safeCopy(ItemStack[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = array[index];
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    /**
     * Return a deep copy of a single stack, or {@link ItemStack#EMPTY} if it is
     * {@code null} or empty.
     */
    private static ItemStack copyOne(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
