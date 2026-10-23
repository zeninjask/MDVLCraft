package com.mdvlcraft.binder.mixin.ef;

import com.mdvlcraft.binder.ability.PhantomRush;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Assassination Rush's assassinations play at double speed (PhantomRush.speed) on both sides. */
@Mixin(value = AnimationPlayer.class, remap = false)
public abstract class AnimationPlayerSpeedMixin {
    @Unique
    private float mdvlcraft$speed = 1.0F;

    @Inject(method = "tick", at = @At("HEAD"))
    private void mdvlcraft$rushSpeed(LivingEntityPatch<?> patch, CallbackInfo ci) {
        this.mdvlcraft$speed = patch.getOriginal() != null ? PhantomRush.speed(patch.getOriginal()) : 1.0F;
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.05F))
    private float mdvlcraft$scaleStep(float step) {
        return step * this.mdvlcraft$speed;
    }
}
