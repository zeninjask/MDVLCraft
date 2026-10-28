package com.mdvlcraft.binder.mixin.farmersdelight;

import com.mdvlcraft.binder.content.WildCooking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/**
 * Wild Cooking's pot (content/WildCooking) stands over no fire but is always heated: both for the screen's flame
 * (isHeated()) and for cooking, whose tick asks HeatableBlockEntity.isHeated(level, pos) for a heat source under the pot.
 */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public abstract class CookingPotHeatMixin {
    @Inject(method = "isHeated()Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$wildPotHeated(CallbackInfoReturnable<Boolean> cir) {
        if (WildCooking.isVirtual((CookingPotBlockEntity)(Object)this)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "cookingTick", at = @At(value = "INVOKE",
        target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean mdvlcraft$wildPotCooks(CookingPotBlockEntity pot, Level level, BlockPos pos) {
        return WildCooking.isVirtual(pot) || pot.isHeated(level, pos);
    }
}
