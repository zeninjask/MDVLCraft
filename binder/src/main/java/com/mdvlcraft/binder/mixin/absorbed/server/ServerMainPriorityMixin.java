package com.mdvlcraft.binder.mixin.absorbed.server;

import com.mdvlcraft.binder.absorbed.SmoothBoot;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** From Smooth Boot (Reloaded): the dedicated server game thread's priority. */
@Mixin(Main.class)
public abstract class ServerMainPriorityMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void mdvlcraft$priority(CallbackInfo ci) {
        Thread.currentThread().setPriority(SmoothBoot.config().threadPriority.game);
    }
}
