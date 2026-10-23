package com.mdvlcraft.binder.integration;

import net.minecraft.world.entity.Mob;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * An Epic Fight patch for Dummmmmmy's target dummy (registered in EpicFightIntegration), so Combat Evolution can
 * assassinate it (it needs a patch on the target). The dummy keeps its own renderer and never moves or staggers.
 */
public class TargetDummyPatch extends MobPatch<Mob> {
    public TargetDummyPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    protected void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        this.currentLivingMotion = LivingMotions.IDLE;
    }

    @Override
    public AssetAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return null;
    }

    @Override
    public boolean overrideRender() {
        return false;
    }
}
