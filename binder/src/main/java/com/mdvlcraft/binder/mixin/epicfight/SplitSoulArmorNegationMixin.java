package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.combat.KatanaWeapons;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * The Split Soul Katana's Epic Fight attacks negate all armour. Epic Fight's own armour step (its LivingEntity mixin) skips
 * Apothic Attributes, so mixin.attributeslib.ALCombatRulesMixin only covers the katana's vanilla hits.
 */
@Mixin(value = EpicFightDamageSource.class, remap = false)
public abstract class SplitSoulArmorNegationMixin {
    @Inject(method = "calculateArmorNegation", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$splitSoul(CallbackInfoReturnable<Float> cir) {
        if (KatanaWeapons.splitSoulHit((DamageSource)(Object)this)) {
            cir.setReturnValue(100.0F);
        }
    }
}
