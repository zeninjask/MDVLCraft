package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.world.capabilities.entitypatch.CustomHumanoidMobPatch;

/**
 * A datapack humanoid patch with no combat_behavior (the mages', data/<mod>/epicfight_mobpatch) keeps the mob's own
 * attacks. Epic Fight fell back to fist attacks for it and put its chase-and-swing goal ahead of the mob's spellcasting.
 */
@Mixin(value = CustomHumanoidMobPatch.class, remap = false)
public abstract class CustomHumanoidMobPatchMixin {
    @Shadow
    @Final
    private MobPatchReloadListener.CustomHumanoidMobPatchProvider provider;

    @Inject(method = "setAIAsInfantry", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$ownAttacks(boolean holdingRangedWeapon, CallbackInfo ci) {
        if (this.provider.getHumanoidCombatBehaviors().isEmpty()) {
            ci.cancel();
        }
    }
}
