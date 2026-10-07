package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.ProjectionDashProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phantom Movement's (the addon's "Don't You Have a Heart?") Continuous mode: the screen shake it gives at speed and
 * the Photon wind-flash bursts (the air effect round the player) are dropped. The trail of projection frames left
 * behind the player stays (spawnSingleFrame, not a command).
 */
@Mixin(value = ProjectionDashProcedure.class, remap = false)
public abstract class ProjectionDashProcedureMixin {
    @Inject(method = "runCommand", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noShakeOrWind(Entity entity, String command, CallbackInfo ci) {
        if (command.contains("cursedfate:shake") || command.contains("photon:windflash1")) {
            ci.cancel();
        }
    }
}
