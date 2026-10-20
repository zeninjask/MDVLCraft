package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.skill.weaponinnate.EviscerateSkill;

@Mixin(value = EviscerateSkill.class, remap = false)
public interface EviscerateSkillAccessor {
    @Accessor("first")
    AssetAccessor<? extends AttackAnimation> mdvlcraft$first();

    @Accessor("second")
    AssetAccessor<? extends AttackAnimation> mdvlcraft$second();
}
