package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.wukong.WukongGate;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** The Ruyi Jingu Bang's moveset only with the Monkey King identity (wukong/WukongGate). */
@Mixin(value = LivingEntityPatch.class, remap = false)
public abstract class LivingEntityPatchHoldingMixin {
    @Inject(method = "getHoldingItemCapability", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$gateWukongStaff(InteractionHand hand, CallbackInfoReturnable<CapabilityItem> cir) {
        cir.setReturnValue(WukongGate.gate((LivingEntityPatch<?>)(Object)this, cir.getReturnValue()));
    }
}
