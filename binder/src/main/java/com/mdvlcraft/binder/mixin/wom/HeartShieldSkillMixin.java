package com.mdvlcraft.binder.mixin.wom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Heart Shield (Weapons of Miracles) regenerates an absorption shield of up to 10 instead of 20. */
@Pseudo
@Mixin(targets = "reascer.wom.skill.passive.HeartShieldSkill", remap = false)
public abstract class HeartShieldSkillMixin {
    @ModifyConstant(method = "updateContainer", constant = @Constant(intValue = 20), require = 1)
    private int mdvlcraft$maxShield(int shield) {
        return 10;
    }
}
