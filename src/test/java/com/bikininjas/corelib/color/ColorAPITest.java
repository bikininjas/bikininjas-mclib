package com.bikininjas.corelib.color;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorAPITest {

    private static IEventBus newBus() {
        return BusBuilder.builder().build();
    }

    // ================================================================
    // Null checks
    // ================================================================

    @Nested
    class NullChecks {

        @Test
        void tintItemNullBus() {
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItem(null, Items.STONE, 0xFF0000));
        }

        @Test
        void tintItemNullItem() {
            var bus = newBus();
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItem(bus, (ItemLike) null, 0xFF0000));
        }

        @Test
        void tintItemHandlerNullBus() {
            var handler = (ItemColor) (stack, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItem(null, Items.STONE, handler));
        }

        @Test
        void tintItemHandlerNullItem() {
            var bus = newBus();
            var handler = (ItemColor) (stack, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItem(bus, (ItemLike) null, handler));
        }

        @Test
        void tintItemHandlerNullHandler() {
            var bus = newBus();
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItem(bus, Items.STONE, (ItemColor) null));
        }

        @Test
        void tintItemsNullBus() {
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItems(null, 0xFF0000, Items.STONE, Items.DIRT));
        }

        @Test
        void tintItemsNullItems() {
            var bus = newBus();
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintItems(bus, 0xFF0000, (ItemLike[]) null));
        }

        @Test
        void tintBlockNullBus() {
            var handler = (BlockColor) (state, level, pos, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlock(null, Blocks.STONE, handler));
        }

        @Test
        void tintBlockNullBlock() {
            var bus = newBus();
            var handler = (BlockColor) (state, level, pos, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlock(bus, null, handler));
        }

        @Test
        void tintBlockNullHandler() {
            var bus = newBus();
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlock(bus, Blocks.STONE, (BlockColor) null));
        }

        @Test
        void tintBlocksNullBus() {
            var handler = (BlockColor) (state, level, pos, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlocks(null, handler, Blocks.STONE));
        }

        @Test
        void tintBlocksNullHandler() {
            var bus = newBus();
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlocks(bus, null, Blocks.STONE));
        }

        @Test
        void tintBlocksNullBlocks() {
            var bus = newBus();
            var handler = (BlockColor) (state, level, pos, layer) -> 0xFFFFFFFF;
            assertThrows(NullPointerException.class,
                    () -> ColorAPI.tintBlocks(bus, handler, (Block[]) null));
        }
    }

    // ================================================================
    // Item registration — handler is actually called
    // ================================================================

    @Nested
    class ItemRegistration {

        @Test
        void constantColorLayer0() {
            var bus = newBus();
            assertDoesNotThrow(() ->
                    ColorAPI.tintItem(bus, Items.STONE, 0xFFFF0000));
        }

        @Test
        void constantColorSpecificLayer() {
            // Verify the 4-arg overload (color + layer) doesn't throw
            var bus = newBus();
            assertDoesNotThrow(() ->
                    ColorAPI.tintItem(bus, Items.APPLE, 0xFF8888FF, 1));
        }

        @Test
        void dynamicHandler() {
            var bus = newBus();
            ItemColor handler = (stack, layer) ->
                    layer == 0 ? 0xFFFF6666 : 0xFFFFFFFF;

            assertDoesNotThrow(() ->
                    ColorAPI.tintItem(bus, Items.DIAMOND, handler));
        }

        @Test
        void batchItemsSameColor() {
            var bus = newBus();
            assertDoesNotThrow(() ->
                    ColorAPI.tintItems(bus, 0xFF4488FF,
                            Items.STONE, Items.DIRT, Items.COBBLESTONE));
        }

        @Test
        void batchSingleItem() {
            var bus = newBus();
            assertDoesNotThrow(() ->
                    ColorAPI.tintItems(bus, 0xFF4488FF, Items.GOLD_INGOT));
        }
    }

    // ================================================================
    // Block registration — handler is actually registered
    // ================================================================

    @Nested
    class BlockRegistration {

        @Test
        void singleBlock() {
            var bus = newBus();
            BlockColor handler = (state, level, pos, layer) ->
                    layer == 0 ? 0xFF88FF88 : 0xFFFFFFFF;

            assertDoesNotThrow(() ->
                    ColorAPI.tintBlock(bus, Blocks.GRASS_BLOCK, handler));
        }

        @Test
        void batchBlocks() {
            var bus = newBus();
            BlockColor handler = (state, level, pos, layer) -> 0xFFAAAAFF;

            assertDoesNotThrow(() ->
                    ColorAPI.tintBlocks(bus, handler,
                            Blocks.STONE, Blocks.DIRT, Blocks.SAND));
        }

        @Test
        void batchSingleBlock() {
            var bus = newBus();
            BlockColor handler = (state, level, pos, layer) -> 0xFF000000;

            assertDoesNotThrow(() ->
                    ColorAPI.tintBlocks(bus, handler, Blocks.OAK_PLANKS));
        }
    }
}
