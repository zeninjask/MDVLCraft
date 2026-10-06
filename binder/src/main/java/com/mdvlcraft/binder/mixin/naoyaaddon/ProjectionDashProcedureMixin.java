package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.ProjectionDashProcedure;
import com.mdvlcraft.binder.ability.ProjectionSorcery;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Don't You Have a Heart?'s continuous dash: its speed reads the cursed energy output through a Cursed Fate command,
 * which the binder answers with the highest output, and the screen shake it gives at speed is dropped.
 */
@Mixin(value = ProjectionDashProcedure.class, remap = false)
public abstract class ProjectionDashProcedureMixin {
    @Inject(method = "getCommandResult", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$maxOutput(Entity entity, String command, CallbackInfoReturnable<String> cir) {
        if (command.endsWith("@s Output")) {
            cir.setReturnValue(ProjectionSorcery.MAX_OUTPUT);
        }
    }

    @Inject(method = "runCommand", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noShake(Entity entity, String command, CallbackInfo ci) {
        if (command.contains("cursedfate:shake")) {
            ci.cancel();
        }
    }
}
