package com.mdvlcraft.binder.mixin.ce;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** CE plays a humanoid "being assassinated" animation on the target; a non-humanoid target (now allowed, see
 * AssassinationTargetsMixin) just takes the hit instead. */
@Mixin(targets = "net.shelmarow.combat_evolution.execution.ExecutionTask", remap = false)
public abstract class ExecutionTaskMixin {
    @Redirect(method = "onStart", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;playAnimationSynchronized(Lyesman/epicfight/api/asset/AssetAccessor;F)V",
        ordinal = 1))
    private void mdvlcraft$humanoidOnly(LivingEntityPatch<?> target, AssetAccessor<?> animation, float transition) {
        if (target.getArmature() instanceof HumanoidArmature) {
            target.playAnimationSynchronized((AssetAccessor)animation, transition);
        }
    }
}
