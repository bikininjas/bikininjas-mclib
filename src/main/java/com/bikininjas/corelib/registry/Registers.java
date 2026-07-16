package com.bikininjas.corelib.registry;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Centralized DeferredRegisters for core-lib.
 * <p>
 * Currently empty (no concrete items/blocks defined in this library).
 * Child mods use these registers when they want to add content through core-lib's
 * registration bus, or they create their own local DeferredRegister instances.
 */
public final class Registers {

    private Registers() {
    }

    // -- Registries ----------------------------------------------------------

    /** Items DeferredRegister (core_lib namespace). */
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(com.bikininjas.corelib.CoreLib.MODID);

    /** Blocks DeferredRegister (core_lib namespace). */
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(com.bikininjas.corelib.CoreLib.MODID);

    /** Block entity types DeferredRegister (core_lib namespace). */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, com.bikininjas.corelib.CoreLib.MODID);

    /** Entity types DeferredRegister (core_lib namespace). */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, com.bikininjas.corelib.CoreLib.MODID);

    // -- Helpers -------------------------------------------------------------

    /**
     * Register a simple item with default properties.
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
    public static <T extends Item> DeferredHolder<Item, T> item(String name, Function<Item.Properties, T> factory) {
        return ITEMS.registerItem(name, factory);
    }

    /**
     * Register a block with an automatic {@link BlockItem}.
     *
     * @return a pair of holders (block, item)
     */
    public static Registration<Block, BlockItem> blockWithItem(String name, Supplier<Block> blockFactory) {
        var block = BLOCKS.register(name, blockFactory);
        var item = ITEMS.registerSimpleBlockItem(name, block);
        return new Registration<>(block, item);
    }

    /**
     * Register a block entity type.
     */
    public static <T extends BlockEntityType<?>> DeferredHolder<BlockEntityType<?>, T> blockEntity(
            String name, Supplier<T> factory) {
        return BLOCK_ENTITY_TYPES.register(name, factory);
    }

    /**
     * Register an entity type.
     */
    public static <T extends EntityType<?>> DeferredHolder<EntityType<?>, T> entity(
            String name, Supplier<T> builder) {
        return ENTITY_TYPES.register(name, builder);
    }

    // -- Value type for paired registrations ---------------------------------

    /**
     * Holds a pair of (block, item) registration results.
     */
    public record Registration<B extends Block, I extends Item>(
            DeferredHolder<Block, B> block,
            DeferredHolder<Item, I> item
    ) {
    }
}
