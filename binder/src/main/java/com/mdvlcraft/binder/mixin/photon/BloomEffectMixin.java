package com.mdvlcraft.binder.mixin.photon;

import com.mdvlcraft.binder.client.PhotonResize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a display resize Photon rebuilds its bloom targets on its next size check, even if the size it remembers matches
 * again (client/PhotonResize): forgetting the remembered width makes the check see a change.
 */
@Mixin(targets = "com.lowdragmc.photon.client.postprocessing.BloomEffect", remap = false)
public abstract class BloomEffectMixin {
    @Shadow
    private static int LAST_WIDTH;

    @Inject(method = "updateScreenSize", at = @At("HEAD"), require = 1)
    private static void mdvlcraft$rebuildAfterResize(CallbackInfo ci) {
        if (PhotonResize.takeStale()) {
            LAST_WIDTH = -1;
        }
    }
}
