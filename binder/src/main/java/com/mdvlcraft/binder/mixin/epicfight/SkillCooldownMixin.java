package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.ability.TechniqueCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Epic Fight skill cooldowns recover faster with Iron's cooldown reduction, like spells and techniques. */
@Mixin(value = Skill.class, remap = false)
public abstract class SkillCooldownMixin {
    @Inject(method = "getCooldownRegenPerSecond", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$cooldownReduction(PlayerPatch<?> patch, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float)(cir.getReturnValue() / TechniqueCooldowns.cooldownMultiplier(patch.getOriginal())));
    }
}
