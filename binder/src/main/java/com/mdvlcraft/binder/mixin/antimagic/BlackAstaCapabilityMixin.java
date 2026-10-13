package com.mdvlcraft.binder.mixin.antimagic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Demonic Assimilation keeps its power but never flies. */
@Pseudo
@Mixin(targets = "com.tiago.antimagic.capability.BlackAstaCapability", remap = false)
public abstract class BlackAstaCapabilityMixin {
    @Inject(method = "isDemonicFlying", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$neverFlying(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
