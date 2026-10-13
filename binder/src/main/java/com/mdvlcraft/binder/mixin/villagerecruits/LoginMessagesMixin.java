package com.mdvlcraft.binder.mixin.villagerecruits;

import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Village Recruits' status and warning messages sent to every player on joining a world are not sent. */
@Pseudo
@Mixin(targets = {
    "com.example.villagerecruits.events.StaleConfigWarning",
    "com.example.villagerecruits.events.ExpansionDistanceWarning",
    "com.example.villagerecruits.events.ExtraClassesStatusMessage",
    "com.example.villagerecruits.events.RoadLevelWarning",
    "com.example.villagerecruits.events.ModIntegrityStatusMessage",
    "com.example.villagerecruits.events.DatapackVerificationWarning",
    "com.example.villagerecruits.events.AnimalCapWarning"
}, remap = false)
public abstract class LoginMessagesMixin {
    @Inject(method = "onPlayerJoin", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noLoginMessage(PlayerLoggedInEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
