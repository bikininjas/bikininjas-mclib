package com.bikininjas.corelib.kit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe static registry of {@link Kit} definitions.
 * <p>
 * All methods are static. No event bus registration.
 */
public final class KitManager {

    private static final Map<String, Kit> kits = new ConcurrentHashMap<>();

    private KitManager() {
    }

    /**
     * Register a kit. If a kit with the same name already exists, it is overwritten.
     */
    public static void register(@NotNull Kit kit) {
        Objects.requireNonNull(kit, "kit must not be null");
        kits.put(kit.name(), kit);
    }

    /**
     * Get a kit by name.
     *
     * @return the kit, or null if not found
     */
    public static @Nullable Kit get(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        return kits.get(name);
    }

    /**
     * Get all registered kit names.
     */
    public static @NotNull List<String> getAll() {
        return List.copyOf(kits.keySet());
    }

    /**
     * Remove a kit by name.
     */
    public static void remove(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        kits.remove(name);
    }

    /**
     * Clear all registered kits.
     */
    public static void clear() {
        kits.clear();
    }

    /**
     * Give a kit to a player. Equips armor if applicable, puts items in inventory.
     *
     * @return true if the kit was found and given, false if the kit name is unknown
     */
    public static boolean give(@NotNull ServerPlayer player, @NotNull String name) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(name, "name must not be null");

        var kit = kits.get(name);
        if (kit == null) {
            return false;
        }

        // Items → inventory
        for (var stack : kit.items()) {
            if (!stack.isEmpty()) {
                player.getInventory().add(stack.copy());
            }
        }

        // Armor → equip if slot empty, otherwise add to inventory
        var armorList = kit.armor();
        if (armorList.size() >= 1 && !armorList.get(0).isEmpty()) {
            giveArmor(player, armorList.get(0).copy(), EquipmentSlot.HEAD);
        }
        if (armorList.size() >= 2 && !armorList.get(1).isEmpty()) {
            giveArmor(player, armorList.get(1).copy(), EquipmentSlot.CHEST);
        }
        if (armorList.size() >= 3 && !armorList.get(2).isEmpty()) {
            giveArmor(player, armorList.get(2).copy(), EquipmentSlot.LEGS);
        }
        if (armorList.size() >= 4 && !armorList.get(3).isEmpty()) {
            giveArmor(player, armorList.get(3).copy(), EquipmentSlot.FEET);
        }

        // Offhand
        var offhand = kit.offhand();
        if (!offhand.isEmpty()) {
            var currentOffhand = player.getOffhandItem();
            if (currentOffhand.isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, offhand.copy());
            } else {
                player.getInventory().add(offhand.copy());
            }
        }

        // Effects
        for (var effect : kit.effects()) {
            player.addEffect(effect);
        }

        return true;
    }

    private static void giveArmor(ServerPlayer player, ItemStack stack, EquipmentSlot slot) {
        var current = player.getItemBySlot(slot);
        if (current.isEmpty()) {
            player.setItemSlot(slot, stack);
        } else {
            player.getInventory().add(stack);
        }
    }
}
