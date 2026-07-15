package com.bikininjas.corelib.datamaps;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

/**
 * Utility class for working with NeoForge Data Maps and Data Components.
 * <p>
 * Data Maps allow attaching data to registry objects (items, blocks, etc.)
 * without modifying the object itself. This is useful for:
 * <ul>
 *   <li>Adding custom behavior to vanilla/third-party items</li>
 *   <li>Mod compat — attaching data maps via JSON in data packs</li>
 *   <li>Separating configuration from code</li>
 * </ul>
 */
public final class DataMapHelper {

    private DataMapHelper() {}

    /**
     * Create a builder for a DataComponentType with a codec.
     */
    public static <T> DataComponentType.Builder<T> componentBuilder() {
        return DataComponentType.builder();
    }

    /**
     * Register a simple DataComponentType with a codec.
     */
    public static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> registerComponent(
            String name,
            Supplier<? extends DataComponentType<T>> supplier
    ) {
        // Components are typically registered via DeferredRegister<DataComponentType<?>>
        // This is a convenience — the actual DeferredRegister should be in the calling mod.
        throw new UnsupportedOperationException(
                "Use DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, modid).register(name, supplier) instead."
        );
    }

    // ──────────────────────────────────────────────
    //  ItemStack helpers
    // ──────────────────────────────────────────────

    /**
     * Safely get a data component value from an ItemStack.
     */
    public static <T> T getComponent(ItemStack stack, DataComponentType<T> type) {
        var value = stack.get(type);
        if (value == null) {
            throw new IllegalArgumentException(
                    "ItemStack is missing required component: " + type);
        }
        return value;
    }

    /**
     * Get a data component or a default value if absent.
     */
    public static <T> T getComponentOrDefault(
            ItemStack stack, DataComponentType<T> type, T defaultValue
    ) {
        var value = stack.get(type);
        return value != null ? value : defaultValue;
    }

    // ──────────────────────────────────────────────
    //  Item (default) helpers
    // ──────────────────────────────────────────────

    /**
     * Get the default data component map for an item.
     */
    public static DataComponentMap getDefaultComponents(Item item) {
        return item.components();
    }

    /**
     * Set a default data component on an item builder.
     */
    public static <T> Item.Properties withComponent(
            Item.Properties props,
            DataComponentType<T> type,
            T value
    ) {
        return props.component(type, value);
    }

    // ──────────────────────────────────────────────
    //  JSON data-map keys
    // ──────────────────────────────────────────────

    /**
     * Generate the JSON data-map key for a component type.
     * <p>
     * Place the resulting file at:
     * {@code data/<mod_id>/datamaps/item/<key>.json}
     */
    public static String dataMapKey(DataComponentType<?> type) {
        var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return id != null ? id.toLanguageKey() : "unknown";
    }
}
