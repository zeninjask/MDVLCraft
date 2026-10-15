package com.mdvlcraft.binder.wukong;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.network.server.SPSkillExecutionFeedback;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;

/**
 * The Ruyi Jingu Bang's innate skill: hold the weapon skill key to wind up, release to smash. Every 15 ticks of charge
 * (up to a minute's worth of holding, capped at 60 ticks) plays the next, longer and harder smash (WukongAnimations.SMASHES).
 */
public class WukongSmashSkill extends WeaponInnateSkill implements ChargeableSkill {
    private static final int TICKS_PER_LEVEL = 15;

    public WukongSmashSkill(SkillBuilder<? extends WeaponInnateSkill> builder) {
        super(builder);
    }

    @Override
    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }

    @Override
    public int getAllowedMaxChargingTicks() {
        return 80;
    }

    @Override
    public int getMaxChargingTicks() {
        return TICKS_PER_LEVEL * (WukongAnimations.SMASHES.size() - 1);
    }

    @Override
    public int getMinChargingTicks() {
        return 1;
    }

    @Override
    public void startHolding(SkillContainer container) {
        container.getExecutor().playAnimationSynchronized(WukongAnimations.SMASH_CHARGING, 0.0F);
    }

    @Override
    public void resetHolding(SkillContainer container) {
        ChargeableSkill.super.resetHolding(container);
        if (container.getExecutor().isLogicalClient()) {
            container.getExecutor().getAnimator().stopPlaying(WukongAnimations.SMASH_CHARGING);
        } else {
            container.getExecutor().stopPlaying(WukongAnimations.SMASH_CHARGING);
        }
    }

    @Override
    public void onStopHolding(SkillContainer container, SPSkillExecutionFeedback feedback) {
        int level = Math.min(container.getExecutor().getAccumulatedChargeAmount() / TICKS_PER_LEVEL, WukongAnimations.SMASHES.size() - 1);
        AnimationAccessor<AttackAnimation> smash = WukongAnimations.SMASHES.get(level);
        container.getExecutor().playAnimationSynchronized(smash, 0.0F);
        this.cancelOnServer(container, null);
    }

    @Override
    public void holdTick(SkillContainer container) {
        ChargeableSkill.super.holdTick(container);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public KeyMapping getKeyMapping() {
        return EpicFightKeyMappings.WEAPON_INNATE_SKILL;
    }
}
