package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.combat.MobWeaponSkills;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/** An armed mob's attacks include its weapon's skill (combat/MobWeaponSkills). */
@Mixin(value = HumanoidMobPatch.class, remap = false)
public abstract class HumanoidMobPatchMixin {
    @Inject(method = "getHoldingItemWeaponMotionBuilder", at = @At("RETURN"), cancellable = true, require = 1)
    private void mdvlcraft$weaponSkill(CallbackInfoReturnable<CombatBehaviors.Builder<HumanoidMobPatch<?>>> cir) {
        if (cir.getReturnValue() != null) {
            cir.setReturnValue(MobWeaponSkills.withSkill((HumanoidMobPatch<?>)(Object)this, cir.getReturnValue()));
        }
    }
}
