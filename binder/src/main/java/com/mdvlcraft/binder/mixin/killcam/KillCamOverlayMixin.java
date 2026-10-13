package com.mdvlcraft.binder.mixin.killcam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kill Cam keeps its slow motion, but its full-screen black calligraphy no longer flashes up on a kill. */
@Pseudo
@Mixin(targets = "org.arc.killcam.client.gui.KillCamOverlay", remap = false)
public abstract class KillCamOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noLetters(CallbackInfo ci) {
        ci.cancel();
    }
}
