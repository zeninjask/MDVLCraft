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
 * A bare Epic Fight patch (registered in EpicFightIntegration) for mobs Epic Fight doesn't patch itself: Dummmmmmy's
 * target dummy and every other mob no mod patches (Cataclysm's, summons, NPCs, animals, ...). Combat Evolution can only
 * assassinate a mob with a patch. The mob keeps its own renderer and AI (initAI does nothing); hits stagger it.
 */
public class PassiveMobPatch extends MobPatch<Mob> {
    public PassiveMobPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    protected void initAI() {
        // the mob's own goals stay
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

    /** Staggered like a biped (a GeckoLib biped shows it through MobRig; other models just lose their footing). */
    @Override
    public AssetAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return switch (stunType) {
            case SHORT -> Animations.BIPED_HIT_SHORT;
            case LONG, HOLD -> Animations.BIPED_HIT_LONG;
            case KNOCKDOWN -> Animations.BIPED_KNOCKDOWN;
            default -> null;
        };
    }

    @Override
    public boolean overrideRender() {
        return false;
    }
}
