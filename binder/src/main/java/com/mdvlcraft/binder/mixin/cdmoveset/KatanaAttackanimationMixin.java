package com.mdvlcraft.binder.mixin.cdmoveset;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * EpicFight Resurrection's katana attacks clear the link animation's transform map, which Epic Fight 20.14 hands out
 * read-only (it crashed the client). The link is reset through its animation clip, as Epic Fight's own animations do.
 */
@Pseudo
@Mixin(targets = "net.corruptdog.cdm.api.animation.types.KatanaAttackanimation", remap = false)
public abstract class KatanaAttackanimationMixin {
    @Redirect(method = "setLinkAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void mdvlcraft$resetClip(Map<?, ?> transforms, AssetAccessor<? extends DynamicAnimation> fromAnimation, Pose startPose,
                                     boolean isOnSameLayer, float transitionTimeModifier, LivingEntityPatch<?> entitypatch, LinkAnimation dest) {
        dest.getAnimationClip().reset();
    }
}
