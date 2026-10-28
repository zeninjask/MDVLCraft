package com.mdvlcraft.binder.mixin.photon;

import com.mdvlcraft.binder.client.PhotonResize;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every display resize (fullscreen on or off, a window resize) marks Photon's bloom targets stale (client/PhotonResize). */
@Mixin(Minecraft.class)
public abstract class MinecraftResizeMixin {
    @Inject(method = "resizeDisplay", at = @At("RETURN"))
    private void mdvlcraft$photonStale(CallbackInfo ci) {
        PhotonResize.markStale();
    }
}
