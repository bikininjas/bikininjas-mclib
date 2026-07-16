package com.bikininjas.corelib.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Fluent builder for creating Minecraft recipe holders programmatically.
 * <p>
 * Supports shaped (3×3), shapeless, and smelting recipes.
 */
public final class RecipeBuilder {

    private RecipeBuilder() {
    }

    // -- Shaped --------------------------------------------------------------

    /**
     * Start building a shaped (3×3 grid) recipe.
     */
    public static @NotNull ShapedBuilder shaped(@NotNull ItemStack result) {
        Objects.requireNonNull(result, "result must not be null");
        return new ShapedBuilder(result);
    }

    // -- Shapeless -----------------------------------------------------------

    /**
     * Start building a shapeless recipe.
     */
    public static @NotNull ShapelessBuilder shapeless(@NotNull ItemStack result) {
        Objects.requireNonNull(result, "result must not be null");
        return new ShapelessBuilder(result);
    }

    // -- Smelting ------------------------------------------------------------

    /**
     * Start building a smelting (furnace) recipe.
     */
    public static @NotNull SmeltingBuilder smelting(@NotNull ItemStack result, @NotNull ItemStack ingredient,
                                                     float experience, int cookingTime) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(ingredient, "ingredient must not be null");
        return new SmeltingBuilder(result, ingredient, experience, cookingTime);
    }

    // -- Builder implementations ---------------------------------------------

    /**
     * Builder for shaped (pattern + ingredients) recipes.
     */
    public static final class ShapedBuilder {
        private final ItemStack result;
        private final List<String> pattern = new ArrayList<>();
        private final Map<Character, Ingredient> keys = new HashMap<>();

        private ShapedBuilder(ItemStack result) {
            this.result = result;
        }

        /**
         * Add a pattern row (e.g. {@code " D ", " D ", " S "}).
         * Up to 3 rows of up to 3 characters each.
         */
        public @NotNull ShapedBuilder pattern(@NotNull String row) {
            Objects.requireNonNull(row, "row must not be null");
            pattern.add(row);
            return this;
        }

        /**
         * Map a character in the pattern to an ingredient.
         */
        public @NotNull ShapedBuilder where(char key, @NotNull Ingredient ingredient) {
            Objects.requireNonNull(ingredient, "ingredient must not be null");
            keys.put(key, ingredient);
            return this;
        }

        /**
         * Map a character in the pattern to a single item (convenience).
         */
        public @NotNull ShapedBuilder where(char key, @NotNull ItemStack item) {
            return where(key, Ingredient.of(item));
        }

        /**
         * Build the recipe holder.
         *
         * @return an Optional containing the recipe, or empty if the builder is in an invalid state
         */
        public @NotNull Optional<RecipeHolder<?>> build() {
            if (pattern.isEmpty() || keys.isEmpty()) {
                return Optional.empty();
            }
            var ingredientList = NonNullList.withSize(pattern.size() * pattern.getFirst().length(), Ingredient.EMPTY);
            for (int r = 0; r < pattern.size(); r++) {
                var row = pattern.get(r);
                for (int c = 0; c < row.length(); c++) {
                    var ing = keys.getOrDefault(row.charAt(c), Ingredient.EMPTY);
                    ingredientList.set(r * row.length() + c, ing);
                }
            }
            var recipe = new ShapedRecipe(
                    "",
                    CraftingBookCategory.MISC,
                    ShapedRecipePattern.of(keys, pattern),
                    result,
                    true
            );
            return Optional.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath("core_lib", "shaped_" + Objects.hash(result)),
                    recipe
            ));
        }
    }

    /**
     * Builder for shapeless recipes.
     */
    public static final class ShapelessBuilder {
        private final ItemStack result;
        private final List<Ingredient> ingredients = new ArrayList<>();

        private ShapelessBuilder(ItemStack result) {
            this.result = result;
        }

        /**
         * Add a required ingredient.
         */
        public @NotNull ShapelessBuilder requires(@NotNull Ingredient ingredient) {
            Objects.requireNonNull(ingredient, "ingredient must not be null");
            ingredients.add(ingredient);
            return this;
        }

        /**
         * Add a required ingredient by item and count.
         */
        public @NotNull ShapelessBuilder requires(@NotNull ItemStack item) {
            return requires(Ingredient.of(item));
        }

        /**
         * Build the recipe holder.
         */
        public @NotNull Optional<RecipeHolder<?>> build() {
            if (ingredients.isEmpty()) {
                return Optional.empty();
            }
            var recipe = new ShapelessRecipe(
                    "",
                    CraftingBookCategory.MISC,
                    result,
                    NonNullList.copyOf(ingredients)
            );
            return Optional.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath("core_lib", "shapeless_" + Objects.hash(result)),
                    recipe
            ));
        }
    }

    /**
     * Builder for smelting recipes.
     */
    public static final class SmeltingBuilder {
        private final ItemStack result;
        private final Ingredient ingredient;
        private final float experience;
        private final int cookingTime;

        private SmeltingBuilder(ItemStack result, ItemStack ingredient, float experience, int cookingTime) {
            this.result = result;
            this.ingredient = Ingredient.of(ingredient);
            this.experience = experience;
            this.cookingTime = cookingTime;
        }

        /**
         * Build the smelting recipe holder.
         */
        public @NotNull Optional<RecipeHolder<?>> build() {
            var recipe = new SmeltingRecipe(
                    "",
                    CookingBookCategory.MISC,
                    ingredient,
                    result,
                    experience,
                    cookingTime
            );
            return Optional.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath("core_lib", "smelting_" + Objects.hash(result)),
                    recipe
            ));
        }
    }
}
