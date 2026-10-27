package com.mdvlcraft.binder.mixin.absorbed.client;

import com.mdvlcraft.binder.absorbed.SmoothBoot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** From Smooth Boot (Reloaded): the singleplayer server thread's priority. */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerPriorityMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void mdvlcraft$priority(CallbackInfo ci) {
        ((MinecraftServer)(Object)this).getRunningThread().setPriority(SmoothBoot.config().threadPriority.integratedServer);
    }
}
