package com.bikininjas.corelib.kit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static, thread-safe registry of {@link Kit}s and the entry point for handing
 * them to players.
 * <p>
 * This is a utility class: it is {@code final}, cannot be instantiated, and
 * exposes only static methods. Kits are stored in a {@link ConcurrentHashMap}
 * keyed by name, so registration and lookup are safe to call from multiple
 * threads (e.g. parallel mod loading or command dispatch).
 * <p>
 * The registry is intentionally decoupled from any event bus, config system or
 * other core-lib module — it has no static initialiser and no runtime
 * dependencies, which keeps it usable from pure unit tests.
 */
public final class KitManager {

    /** Registered kits keyed by name. Concurrent for safe multi-thread access. */
    private static final ConcurrentHashMap<String, Kit> registry = new ConcurrentHashMap<>();

    /** Utility class — never instantiated. */
    private KitManager() {
    }

    // ──────────────────────────────────────────────
    //  Registration
    // ──────────────────────────────────────────────

    /**
     * Register (or overwrite) a kit under the given name.
     *
     * @param name the registry key (non-null, non-blank)
     * @param kit  the kit to store (non-null)
     * @throws NullPointerException     if {@code name} or {@code kit} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static void register(String name, Kit kit) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kit, "kit must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        registry.put(name, kit);
    }

    /**
     * Look up a registered kit by name.
     *
     * @param name the registry key (non-null)
     * @return the kit, or {@code null} if no kit is registered under that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static Kit get(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return registry.get(name);
    }

    /**
     * @return a snapshot copy of all registered kit names (never {@code null})
     */
    public static Collection<String> getAll() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /**
     * Remove a kit from the registry.
     * <p>
     * Removing a name that is not registered is a no-op and does not throw.
     *
     * @param name the registry key to remove (non-null)
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static void remove(String name) {
        Objects.requireNonNull(name, "name must not be null");
        registry.remove(name);
    }

    /**
     * Remove every kit from the registry.
     */
    public static void clear() {
        registry.clear();
    }

    // ──────────────────────────────────────────────
    //  Application
    // ──────────────────────────────────────────────

    /**
     * Hand a registered kit to a player, fully replacing their current loadout.
     * <p>
     * The player's inventory is cleared first, then the kit's main items are
     * placed into the first available inventory slots (extras are dropped if the
     * inventory is full), the four armor pieces are set into their dedicated
     * slots, the offhand item is equipped, and every status effect is applied.
     *
     * @param player the target player (non-null)
     * @param name   the registry key of the kit to give (non-null)
     * @return {@code true} if the kit was found and applied, {@code false} if no
     *         kit is registered under {@code name}
     * @throws NullPointerException if {@code player} or {@code name} is {@code null}
     */
    public static boolean give(ServerPlayer player, String name) {
        Objects.requireNonNull(name, "name must not be null");

        Kit kit = registry.get(name);
        if (kit == null) {
            return false;
        }

        // Validate the player only once we know a kit exists and we are about
        // to mutate its inventory — an unknown name still returns false.
        Objects.requireNonNull(player, "player must not be null");

        var inventory = player.getInventory();

        // Clear the existing loadout (main + armor + offhand).
        inventory.clearContent();

        // Main inventory items — fill the first 36 slots, drop the rest.
        if (kit.items() != null) {
            int maxSlots = Math.min(inventory.getContainerSize(), 36);
            int slot = 0;
            for (ItemStack item : kit.items()) {
                if (item == null || item.isEmpty()) {
                    continue;
                }
                if (slot < maxSlots) {
                    inventory.setItem(slot, item.copy());
                    slot++;
                } else {
                    player.drop(item.copy(), true);
                }
            }
        }

        // Armor — indexed as [boots, leggings, chestplate, helmet].
        if (kit.armor() != null) {
            for (int i = 0; i < 4 && i < kit.armor().length; i++) {
                ItemStack piece = kit.armor()[i];
                if (piece != null && !piece.isEmpty()) {
                    inventory.armor.set(i, piece.copy());
                }
            }
        }

        // Offhand.
        if (kit.offhand() != null && !kit.offhand().isEmpty()) {
            inventory.offhand.set(0, kit.offhand().copy());
        }

        // Status effects.
        if (kit.effects() != null) {
            for (MobEffectInstance effect : kit.effects()) {
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect));
                }
            }
        }

        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
