package com.mdvlcraft.binder.mixin;

import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Armour and protection enchantments block 35% less of the damage they would (Epic Fight's armour path calls these too). */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {
    private static final float REDUCTION_SCALE = 0.65F;

    @Inject(method = "getDamageAfterAbsorb", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$weakerArmour(float damage, float armor, float toughness, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(damage - (damage - cir.getReturnValue()) * REDUCTION_SCALE);
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$weakerProtection(float damage, float enchantModifiers, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(damage - (damage - cir.getReturnValue()) * REDUCTION_SCALE);
    }
}
