package com.mdvlcraft.binder.mixin.jer;

import com.mdvlcraft.binder.compat.jer.JerLoot;
import jeresources.compatibility.minecraft.MinecraftCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** JER's mob and chest pages come from the server's loot views (compat/jer/JerLoot); its render hooks for vanilla mobs stay. */
@Mixin(value = MinecraftCompat.class, remap = false)
public abstract class MinecraftCompatMixin {
    @Inject(method = "registerVanillaMobs", at = @At("TAIL"))
    private void mdvlcraft$serverMobs(CallbackInfo ci) {
        JerLoot.registerMobs();
    }

    @Inject(method = "registerDungeonLoot", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$serverChests(CallbackInfo ci) {
        JerLoot.registerChests();
        ci.cancel();
    }
}
