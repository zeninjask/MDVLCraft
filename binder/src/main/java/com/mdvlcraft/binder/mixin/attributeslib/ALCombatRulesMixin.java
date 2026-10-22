package com.mdvlcraft.binder.mixin.attributeslib;

import com.mdvlcraft.binder.combat.WeakerArmour;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Apothic Attributes replaces vanilla's armour and protection maths (LivingEntity calls these instead of CombatRules),
 * so the weaker armour of CombatRulesMixin is applied here too: armour and protection enchantments block 35% less.
 */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.attributeslib.api.ALCombatRules", remap = false)
public abstract class ALCombatRulesMixin {
    @Inject(method = "getDamageAfterArmor", at = @At("RETURN"), cancellable = true, require = 1)
    private static void mdvlcraft$weakerArmour(LivingEntity target, DamageSource source, float damage, float armor, float toughness,
                                               CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(WeakerArmour.scale(damage, cir.getReturnValue()));
    }

    @Inject(method = "getDamageAfterProtection", at = @At("RETURN"), cancellable = true, require = 1)
    private static void mdvlcraft$weakerProtection(LivingEntity target, DamageSource source, float damage, float protection,
                                                   CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(WeakerArmour.scale(damage, cir.getReturnValue()));
    }
}
