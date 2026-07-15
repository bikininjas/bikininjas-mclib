package com.bikininjas.corelib.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Boosts the enchanting-table level output so players can reach high enchant
 * levels (up to {@link #MAX_LEVEL}) without needing an impractical number of
 * bookshelves.
 * <p>
 * Note: in NeoForge 1.21.1 the offered enchantment levels are <em>not</em>
 * exposed through a {@code getEnchantmentLevels()} getter (that method only
 * exists on the Fabric screen handler). Instead they are computed in
 * {@link EnchantmentMenu#slotsChanged(Container)} and stored in the public
 * {@code costs} array. We multiply each non-zero value by {@link #MULTIPLIER}
 * and clamp it to {@link #MAX_LEVEL}.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    /** Factor applied to each computed enchantment level. */
    private static final int MULTIPLIER = 3;

    /** Hard cap applied to every boosted level. */
    private static final int MAX_LEVEL = 100;

    /**
     * Multiply and clamp the three enchantment levels after they are computed.
     */
    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void onSlotsChanged(CallbackInfo ci) {
        EnchantmentMenu self = (EnchantmentMenu) (Object) this;
        for (int i = 0; i < self.costs.length; i++) {
            if (self.costs[i] > 0) {
                self.costs[i] = Math.min(self.costs[i] * MULTIPLIER, MAX_LEVEL);
            }
        }
    }
}
