package com.mdvlcraft.binder.mixin.jer;

import com.mdvlcraft.binder.compat.jer.JerLoot;
import jeresources.registry.VillagerRegistry;
import jeresources.util.VillagersHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** JER reads villager trades on the client, which only has the modded ones once compat/jer/JerLoot loads them. */
@Mixin(value = VillagersHelper.class, remap = false)
public abstract class VillagersHelperMixin {
    @Inject(method = "initRegistry", at = @At("HEAD"))
    private static void mdvlcraft$forgeTrades(VillagerRegistry registry, CallbackInfo ci) {
        JerLoot.loadTrades();
    }
}
