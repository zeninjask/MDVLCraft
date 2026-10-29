package com.mdvlcraft.binder.mixin.attributeslib;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apothic Attributes' own critical-strike roll is off: combat/Crits rolls once for every hit and reads its attributes. */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.attributeslib.impl.AttributeEvents", remap = false)
public abstract class ApothCritMixin {
    @Inject(method = "apothCriticalStrike", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$noOwnCrit(LivingHurtEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
