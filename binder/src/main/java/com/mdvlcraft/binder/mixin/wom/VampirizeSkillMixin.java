package com.mdvlcraft.binder.mixin.wom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Vampirize (Weapons of Miracles) heals 12% of the damage dealt instead of 20%. */
@Pseudo
@Mixin(targets = "reascer.wom.skill.passive.VampirizeSkill", remap = false)
public abstract class VampirizeSkillMixin {
    @ModifyConstant(method = "lambda$onInitiate$0", constant = @Constant(floatValue = 0.2F), require = 1)
    private static float mdvlcraft$lifeSteal(float share) {
        return 0.12F;
    }
}
