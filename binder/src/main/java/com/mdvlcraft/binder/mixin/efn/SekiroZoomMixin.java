package com.mdvlcraft.binder.mixin.efn;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * A Sekiro move's zoom pulse (VIX's FOV pulse) is a client event of the animation, so it zoomed the screen of every player
 * who saw the move; now only the player doing it gets it.
 */
@Pseudo
@Mixin(targets = {"com.hm.efn.gameasset.animations.EFNSekiroAnimations"}, remap = false)
public abstract class SekiroZoomMixin {
    @Inject(method = "lambda$build$29", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$onlyOwnZoom(LivingEntityPatch<?> patch, AssetAccessor<?> self, AnimationParameters params, CallbackInfo ci) {
        if (patch.getOriginal() != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
