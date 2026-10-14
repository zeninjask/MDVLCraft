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
 * Feral Claw's Beast Roar zoom pulse (VIX's FOV pulse) is a client event of the animation, so it zoomed the screen of every player
 * who saw the move; now only the player doing it gets it.
 */
@Pseudo
@Mixin(targets = {"com.hm.efn.gameasset.animations.EFNClawAnimations", "com.hm.efn.gameasset.animations.EFNClawAnimations_N"}, remap = false)
public abstract class ClawZoomMixin {
    @Inject(method = "lambda$build$16", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$onlyOwnZoom(LivingEntityPatch<?> patch, AssetAccessor<?> self, AnimationParameters params, CallbackInfo ci) {
        if (patch.getOriginal() != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
