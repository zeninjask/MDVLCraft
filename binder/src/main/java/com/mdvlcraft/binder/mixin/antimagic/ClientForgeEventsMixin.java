package com.mdvlcraft.binder.mixin.antimagic;

import net.minecraftforge.client.event.MovementInputUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mid-air jump that toggled Demonic Assimilation's flight (and started gliding on the client) does nothing. */
@Pseudo
@Mixin(targets = "com.tiago.antimagic.client.ClientForgeEvents", remap = false)
public abstract class ClientForgeEventsMixin {
    @Inject(method = "onMovementInput", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noFlightToggle(MovementInputUpdateEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
