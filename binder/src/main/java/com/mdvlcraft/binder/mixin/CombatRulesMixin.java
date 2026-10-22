package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.combat.WeakerArmour;
import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Armour and protection enchantments block 35% less of the damage they would. With Apothic Attributes in the pack,
 * entities use its maths instead (attributeslib.ALCombatRulesMixin applies the same scale there).
 */
@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {
    @Inject(method = "getDamageAfterAbsorb", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$weakerArmour(float damage, float armor, float toughness, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(WeakerArmour.scale(damage, cir.getReturnValue()));
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$weakerProtection(float damage, float enchantModifiers, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(WeakerArmour.scale(damage, cir.getReturnValue()));
    }
}
