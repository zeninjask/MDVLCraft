package com.mdvlcraft.binder.mixin.ef;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

/**
 * Epic Fight's Ender Dragon asks its armature for joints it doesn't have (a null leg joint for its ground-phase IK,
 * Mouth_Upper in the air strike) and threw on every tick and frame of the fight: thousands of logged errors, and CrashGuard
 * removed the client's dragon after 100 failed ticks. A missing joint now gets no transform instead.
 */
@Mixin(value = Armature.class, remap = false)
public abstract class ArmatureJointGuardMixin {
    @Shadow
    public abstract boolean hasJoint(String name);

    @Inject(method = "getBoundTransformFor", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$missingJoint(Pose pose, Joint joint, CallbackInfoReturnable<OpenMatrix4f> cir) {
        if (joint == null || !this.hasJoint(joint.getName())) {
            cir.setReturnValue(new OpenMatrix4f());
        }
    }
}
