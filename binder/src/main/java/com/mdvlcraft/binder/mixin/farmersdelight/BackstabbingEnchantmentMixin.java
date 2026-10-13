package com.mdvlcraft.binder.mixin.farmersdelight;

import com.mdvlcraft.binder.combat.EnchantmentScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Backstabbing adds 80% of its extra damage. */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.item.enchantment.BackstabbingEnchantment", remap = false)
public abstract class BackstabbingEnchantmentMixin {
    @Inject(method = "getBackstabbingDamagePerLevel", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$scaleBackstab(float amount, int level, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(amount + (cir.getReturnValue() - amount) * EnchantmentScaling.SCALE);
    }
}
