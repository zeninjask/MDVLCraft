package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.skill.weaponinnate.GraspingSpireSkill;

@Mixin(value = GraspingSpireSkill.class, remap = false)
public interface GraspingSpireSkillAccessor {
    @Accessor("first")
    AnimationManager.AnimationAccessor<? extends AttackAnimation> mdvlcraft$first();

    @Accessor("second")
    AnimationManager.AnimationAccessor<? extends AttackAnimation> mdvlcraft$second();
}
