package com.mdvlcraft.binder.mixin.distanthorizons;

import com.mdvlcraft.binder.mixin.chunky.ChunkyProviderAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distant Horizons binds to Chunky's generation events on the first chunk update, but Chunky only exists once a server
 * has started: on a dedicated server the spawn chunks come first, and a client on a remote server never starts Chunky.
 * DH marks the binding done before trying it, so Chunky's "not loaded" error crashed the game. Until Chunky is loaded
 * the setup is skipped (and asked again on the next chunk update), so DH still pauses its own generation while Chunky
 * pre-generates.
 */
@Mixin(value = AbstractChunkyAccessor.class, remap = false)
public abstract class AbstractChunkyAccessorMixin {
    @Inject(method = "tryRunFirstTimeSetup", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$waitForChunky(CallbackInfo ci) {
        if (ChunkyProviderAccessor.mdvlcraft$instance() == null) {
            ci.cancel();
        }
    }
}
