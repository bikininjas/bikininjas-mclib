package com.bikininjas.corelib.restriction;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for blocking specific actions: block placement/breaking, item usage,
 * entity spawning, and dimension entry.
 * <p>
 * All methods are static. Automatically registers event handlers via static initializer.
 */
public final class RestrictionManager {

    private static final Map<RestrictionType, Set<ResourceLocation>> restrictions = new EnumMap<>(RestrictionType.class);

    static {
        for (var type : RestrictionType.values()) {
            restrictions.put(type, ConcurrentHashMap.newKeySet());
        }
        NeoForge.EVENT_BUS.register(RestrictionHandler.class);
    }

    private RestrictionManager() {
    }

    // -- Registration --------------------------------------------------------

    /**
     * Register a restriction: block the given resource from the specified action.
     */
    public static void register(@NotNull RestrictionType type, @NotNull ResourceLocation id) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        restrictions.get(type).add(id);
    }

    /**
     * Register a restriction using namespace and path (e.g. {@code "minecraft", "tnt"}).
     */
    public static void register(@NotNull RestrictionType type, @NotNull String namespace, @NotNull String path) {
        register(type, ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(namespace, "namespace must not be null"),
                Objects.requireNonNull(path, "path must not be null")));
    }

    // -- Query ---------------------------------------------------------------

    /**
     * Check whether the given resource is restricted for the specified action type.
     */
    public static boolean isRestricted(@NotNull RestrictionType type, @NotNull ResourceLocation id) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        return restrictions.get(type).contains(id);
    }

    // -- Removal -------------------------------------------------------------

    /**
     * Remove a specific restriction.
     */
    public static void unregister(@NotNull RestrictionType type, @NotNull ResourceLocation id) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        restrictions.get(type).remove(id);
    }

    /**
     * Remove all restrictions of a given type.
     */
    public static void clear(@NotNull RestrictionType type) {
        Objects.requireNonNull(type, "type must not be null");
        restrictions.get(type).clear();
    }

    /**
     * Remove all restrictions of all types.
     */
    public static void clear() {
        for (var set : restrictions.values()) {
            set.clear();
        }
    }

    /**
     * Get all restricted resources for a given type (unmodifiable view).
     */
    public static @NotNull Set<ResourceLocation> getAll(@NotNull RestrictionType type) {
        return Collections.unmodifiableSet(restrictions.get(type));
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     */
    public static void init() {
        // static initializer already ran; this method exists for explicit invocation
    }

    // -- Event handler -------------------------------------------------------

    private static final class RestrictionHandler {
        private RestrictionHandler() {
        }

        @SubscribeEvent
        static void onPlaceBlock(@NotNull BlockEvent.EntityPlaceEvent event) {
            var pos = event.getPos();
            var id = event.getPlacedBlock().getBlock().builtInRegistryHolder().key().location();
            if (isRestricted(RestrictionType.PLACE_BLOCK, id)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        static void onBreakBlock(@NotNull BlockEvent.BreakEvent event) {
            var state = event.getState();
            var id = state.getBlock().builtInRegistryHolder().key().location();
            if (isRestricted(RestrictionType.BREAK_BLOCK, id)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        static void onUseItem(@NotNull PlayerInteractEvent.RightClickItem event) {
            var stack = event.getItemStack();
            var id = stack.getItem().builtInRegistryHolder().key().location();
            if (isRestricted(RestrictionType.USE_ITEM, id)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        static void onEntitySpawn(@NotNull EntityJoinLevelEvent event) {
            var id = event.getEntity().getType().builtInRegistryHolder().key().location();
            if (isRestricted(RestrictionType.SPAWN_ENTITY, id)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        static void onTravelToDimension(@NotNull EntityTravelToDimensionEvent event) {
            var id = event.getDimension().location();
            if (isRestricted(RestrictionType.ENTER_DIMENSION, id)) {
                event.setCanceled(true);
            }
        }
    }
}
