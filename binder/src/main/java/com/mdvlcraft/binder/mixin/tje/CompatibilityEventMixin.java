package com.mdvlcraft.binder.mixin.tje;

import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trajectory Estimation is in the pack only to draw the Haunted Lord's leap path (client/LeapTrajectory, which calls its
 * drawing helpers itself): its own trajectories for bows, crossbows, tridents, pearls, potions and the modded items, and
 * its "ding" when a mob is in the way, are never registered. Pseudo: a client without the mod starts as usual.
 */
@Pseudo
@Mixin(targets = "sonnenlichts.tje.client.event.CompatibilityEvent", remap = false)
public abstract class CompatibilityEventMixin {
    @Inject(method = "interModEnqueue", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mdvlcraft$onlyTheLeap(InterModEnqueueEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
