package com.mdvlcraft.binder.mixin.killcam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kill Cam is switched off: its kill cinematic (slow motion, the zoom and camera shake, the full-screen black
 * calligraphy, the invulnerability) never starts. Every trigger, vanilla or Epic Fight kill, goes through triggerTimeSlow.
 */
@Pseudo
@Mixin(targets = "org.arc.killcam.util.KillCamUtil", remap = false)
public abstract class KillCamUtilMixin {
    @Inject(method = "triggerTimeSlow", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noKillCam(CallbackInfo ci) {
        ci.cancel();
    }
}
