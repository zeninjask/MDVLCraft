package com.mdvlcraft.binder.integration;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;

/**
 * Willow's Epic Fight patch: she stands and walks on Epic Fight's biped animations, as Epic Fight (and Epic Fight Extra)
 * animate the vanilla bipeds, instead of her own. MobRig puts the pose onto her GeckoLib model all the time, not only
 * while she is staggered.
 */
public class WillowPatch extends PassiveMobPatch {
    @Override
    protected void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        double dx = this.original.getX() - this.original.xo;
        double dz = this.original.getZ() - this.original.zo;
        this.currentLivingMotion = dx * dx + dz * dz > 1.0E-4 ? LivingMotions.WALK : LivingMotions.IDLE;
    }
}
