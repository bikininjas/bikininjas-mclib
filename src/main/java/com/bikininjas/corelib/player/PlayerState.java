package com.bikininjas.corelib.player;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a player's full state: inventory, health, food, XP, effects, game mode.
 * <p>
 * Created by {@link PlayerStateManager#save} and restored by {@link PlayerStateManager#load}.
 */
public record PlayerState(
        @NotNull List<ItemStack> mainInventory,
        @NotNull List<ItemStack> armorInventory,
        @NotNull ItemStack offhand,
        float health,
        int food,
        float saturation,
        int xpLevel,
        float xpProgress,
        @NotNull List<MobEffectInstance> effects,
        @NotNull GameType gameType
) {

    /**
     * Factory: capture the current state of a player.
     */
    public static @NotNull PlayerState capture(@NotNull Player player) {
        Objects.requireNonNull(player, "player must not be null");
        var inv = player.getInventory();

        var main = new ArrayList<ItemStack>(Inventory.INVENTORY_SIZE - 4);
        for (int i = 0; i < Inventory.INVENTORY_SIZE - 4; i++) {
            main.add(inv.getItem(i).copy());
        }

        var armor = new ArrayList<ItemStack>(4);
        for (int i = 0; i < 4; i++) {
            armor.add(inv.getArmor(i).copy());
        }

        return new PlayerState(
                Collections.unmodifiableList(main),
                Collections.unmodifiableList(armor),
                inv.offhand.get(0).copy(),
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.experienceLevel,
                player.experienceProgress,
                List.copyOf(player.getActiveEffects()),
                player instanceof ServerPlayer sp ? sp.gameMode.getGameModeForPlayer() : GameType.SURVIVAL
        );
    }
}
