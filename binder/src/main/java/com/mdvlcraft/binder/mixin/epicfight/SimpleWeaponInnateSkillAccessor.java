package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.skill.weaponinnate.SimpleWeaponInnateSkill;

@Mixin(value = SimpleWeaponInnateSkill.class, remap = false)
public interface SimpleWeaponInnateSkillAccessor {
    @Accessor("attackAnimation")
    AnimationManager.AnimationAccessor<? extends AttackAnimation> mdvlcraft$animation();
}
