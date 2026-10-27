package com.mdvlcraft.binder.mixin.farmersdelight;

import com.mdvlcraft.binder.content.WildCooking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/** Wild Cooking's pot (content/WildCooking) stands over no fire but is always heated. */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public abstract class CookingPotHeatMixin {
    @Inject(method = "isHeated", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$wildPotHeated(CallbackInfoReturnable<Boolean> cir) {
        if (WildCooking.isVirtual((CookingPotBlockEntity)(Object)this)) {
            cir.setReturnValue(true);
        }
    }
}
