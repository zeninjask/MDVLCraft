package com.mdvlcraft.binder.mixin.alshanex;

import net.alshanex.alshanex_familiars.entity.ShadowEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Call the Shadows' shadows can be hurt and killed, and are tanks: high health and armour, low damage (was 20 / 2 / 6). */
@Mixin(value = ShadowEntity.class, remap = false)
public abstract class ShadowEntityMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void mdvlcraft$tankStats(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 12.0).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Inject(method = "m_6673_", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$hurtable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
