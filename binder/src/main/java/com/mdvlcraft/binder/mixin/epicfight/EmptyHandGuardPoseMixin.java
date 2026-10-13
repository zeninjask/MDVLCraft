package com.mdvlcraft.binder.mixin.epicfight;

import java.util.Map;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** Bare hands have no guard pose, so the Monk's unarmed guard (monk/UnarmedParry) holds a raised two-arm block. */
@Mixin(value = CapabilityItem.class, remap = false)
public abstract class EmptyHandGuardPoseMixin {
    @Inject(method = "getLivingMotionModifier", at = @At("RETURN"))
    private void mdvlcraft$fistGuardPose(
        LivingEntityPatch<?> patch, InteractionHand hand, CallbackInfoReturnable<Map<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> cir
    ) {
        if ((Object)this == CapabilityItem.EMPTY && hand == InteractionHand.MAIN_HAND) {
            cir.getReturnValue().put(LivingMotions.BLOCK, Animations.SWORD_DUAL_GUARD);
        }
    }
}
