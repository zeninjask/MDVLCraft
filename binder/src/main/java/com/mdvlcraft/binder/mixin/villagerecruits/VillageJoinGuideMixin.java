package com.mdvlcraft.binder.mixin.villagerecruits;

import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Village Recruits' join guide is not sent on joining a world (see LoginMessagesMixin). */
@Pseudo
@Mixin(targets = "com.example.villagerecruits.faction.VillageJoinGuide", remap = false)
public abstract class VillageJoinGuideMixin {
    @Inject(method = "onPlayerJoin", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noJoinGuide(PlayerLoggedInEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
