package com.mdvlcraft.binder.mixin.stamina;

import Gvoper.reworked.handlers.StaminaHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Attacks cost half the stamina. The attack cost is attackDamage * 0.75 + weaponWeight * 1.8 (in the basic-attack
 * listener); halving both factors halves every attack, including weapons the stamina config doesn't list.
 */
@Mixin(value = StaminaHandler.class, remap = false)
public abstract class StaminaHandlerMixin {
    @ModifyConstant(method = "lambda$ensureEventListenersInitialized$0", constant = @Constant(doubleValue = 0.75), require = 1)
    private static double mdvlcraft$halveDamageFactor(double factor) {
        return 0.375;
    }

    @ModifyConstant(method = "lambda$ensureEventListenersInitialized$0", constant = @Constant(floatValue = 1.8F), require = 1)
    private static float mdvlcraft$halveWeightFactor(float factor) {
        return 0.9F;
    }
}
