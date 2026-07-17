package com.bikininjas.corelib.loot;

import com.bikininjas.corelib.log.LogManager;
import com.bikininjas.corelib.log.ModLogger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Programmatic loot table injection.
 * <p>
 * Register pending injections via {@link #addToLootTable} and apply them
 * during {@link net.neoforged.neoforge.event.LootTableLoadEvent} (handled
 * in {@link com.bikininjas.corelib.CoreLib}'s constructor).
 * <p>
 * All methods are static. No event bus — registration is done externally.
 */
public final class LootTableHelper {

    private static final ModLogger LOGGER = LogManager.getLogger("core_lib", LootTableHelper.class);

    /** lootTableId → list of LootEntry to inject */
    private static final Map<ResourceLocation, List<LootEntry>> INJECTIONS = new ConcurrentHashMap<>();

    private LootTableHelper() {
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     */
    public static void init() {
    }

    // -- Internal data record ------------------------------------------------

    private record LootEntry(@NotNull ResourceLocation itemId, float weight, int minCount, int maxCount) {
        LootEntry {
            Objects.requireNonNull(itemId, "itemId must not be null");
        }
    }

    // -- Public API ----------------------------------------------------------

    /**
     * Register a loot table injection.
     * <p>
     * When the given loot table is loaded, the specified item will be added
     * as a new pool entry with the configured weight and count range.
     *
     * @param lootTableId the loot table to modify (e.g. {@code minecraft:chests/simple_dungeon})
     * @param itemId      the item to inject (e.g. {@code minecraft:diamond})
     * @param weight      loot pool entry weight
     * @param minCount    minimum item count
     * @param maxCount    maximum item count
     */
    public static void addToLootTable(@NotNull ResourceLocation lootTableId,
                                       @NotNull ResourceLocation itemId,
                                       float weight,
                                       int minCount,
                                       int maxCount) {
        Objects.requireNonNull(lootTableId, "lootTableId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");

        INJECTIONS.computeIfAbsent(lootTableId, k -> new CopyOnWriteArrayList<>())
                  .add(new LootEntry(itemId, weight, minCount, maxCount));
        LOGGER.debug("Registered loot injection: {} → {}", lootTableId, itemId);
    }

    /**
     * Returns an unmodifiable snapshot of current injections.
     */
    public static @NotNull Map<ResourceLocation, List<LootEntry>> getInjections() {
        return Collections.unmodifiableMap(INJECTIONS);
    }

    /**
     * Apply all registered injections to the given loot table.
     * Called from the {@code LootTableLoadEvent} listener in CoreLib.
     *
     * @param lootTableId the ID of the loot table being loaded
     * @param lootTable   the loaded loot table to modify
     */
    public static void injectInto(@NotNull ResourceLocation lootTableId,
                                   @NotNull LootTable lootTable) {
        Objects.requireNonNull(lootTableId, "lootTableId must not be null");
        Objects.requireNonNull(lootTable, "lootTable must not be null");

        var entries = INJECTIONS.get(lootTableId);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (var entry : entries) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.itemId());
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                LOGGER.warn("Unknown item {} for loot table injection — skipping", entry.itemId());
                continue;
            }
            LootPool pool = LootPool.lootPool()
                    .name("core_lib_injected_" + entry.itemId().getPath())
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(item)
                            .setWeight(Math.max(1, Math.round(entry.weight())))
                            .apply(SetItemCountFunction.setCount(
                                    UniformGenerator.between(entry.minCount(), entry.maxCount()))
                            )
                    )
                    .build();

            lootTable.addPool(pool);
            LOGGER.debug("Injected {} into loot table {}", entry.itemId(), lootTableId);
        }
    }
}
