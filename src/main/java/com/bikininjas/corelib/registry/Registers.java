package com.bikininjas.corelib.registry;

import com.bikininjas.corelib.CoreLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Central registry holder for all CoreLib content.
 * <p>
 * Each DeferredRegister is created once here and can be referenced from
 * child mods via {@code Registers.ITEMS}, {@code Registers.BLOCKS}, etc.
 * Child mods register their own content through their own DeferredRegisters.
 */
public final class Registers {

    private Registers() {}

    // ──────────────────────────────────────────────
    //  Main registries
    // ──────────────────────────────────────────────

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoreLib.MODID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CoreLib.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CoreLib.MODID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CoreLib.MODID);

    // ──────────────────────────────────────────────
    //  Convenience helpers
    // ──────────────────────────────────────────────

    /**
     * Register a simple item with no special properties.
     */
    public static DeferredHolder<Item, Item> simpleItem(String name) {
        return ITEMS.registerSimpleItem(name);
    }

    /**
     * Register an item with custom properties.
     */
    public static DeferredHolder<Item, Item> item(String name, Item.Properties properties) {
        return ITEMS.registerItem(name, Item::new, properties);
    }

    /**
     * Register an item with a custom factory.
     */
    public static <I extends Item> DeferredHolder<Item, I> item(
            String name, Supplier<? extends I> factory
    ) {
        return ITEMS.register(name, factory);
    }

    /**
     * Register a block and its corresponding BlockItem in one call.
     *
     * @return a pair containing [block holder, item holder]
     */
    public static Registration<Block, Item> blockWithItem(
            String name, Supplier<? extends Block> blockFactory
    ) {
        var blockHolder = BLOCKS.register(name, blockFactory);
        var itemHolder = ITEMS.register(name,
                () -> new BlockItem(blockHolder.get(), new Item.Properties()));
        return new Registration<Block, Item>(blockHolder, itemHolder);
    }

    /**
     * Register a block entity type.
     */
    public static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> blockEntity(
            String name, Supplier<? extends BlockEntityType<T>> factory
    ) {
        @SuppressWarnings("unchecked")
        var holder = (DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>)
                (DeferredHolder<?, ?>) BLOCK_ENTITY_TYPES.register(name, factory);
        return holder;
    }

    /**
     * Register an entity type.
     */
    public static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> entity(
            String name, EntityType.Builder<T> builder
    ) {
        @SuppressWarnings("unchecked")
        var holder = (DeferredHolder<EntityType<?>, EntityType<T>>)
                (DeferredHolder<?, ?>) ENTITY_TYPES.register(name, () -> builder.build(name));
        return holder;
    }

    // ──────────────────────────────────────────────
    //  Result carrier
    // ──────────────────────────────────────────────

    public record Registration<B extends Block, I extends Item>(
            DeferredHolder<Block, ? extends B> block,
            DeferredHolder<Item, ? extends I> item
    ) {}
}
