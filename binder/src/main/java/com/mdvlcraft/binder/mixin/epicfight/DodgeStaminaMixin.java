package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Dodge skills (Roll, Step and every addon's dodge) cost 10% less stamina. */
@Mixin(value = Skill.class, remap = false)
public abstract class DodgeStaminaMixin {
    private static final float DODGE_STAMINA = 0.9F;

    @Inject(method = "getDefaultConsumptionAmount", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$cheaperDodge(PlayerPatch<?> executor, CallbackInfoReturnable<Float> cir) {
        Skill skill = (Skill)(Object)this;
        if (skill.getCategory() == SkillCategories.DODGE && skill.getResourceType() == Skill.Resource.STAMINA) {
            cir.setReturnValue(cir.getReturnValue() * DODGE_STAMINA);
        }
    }
}
