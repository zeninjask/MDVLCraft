package com.mdvlcraft.binder.mixin.flight;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No armour grants flight: Alex's Caves' Cloak of Darkness no longer turns its wearer into Darkness Incarnate (free
 * flight) on its armour key. The effect from the Darkened Apple is untouched.
 */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.DarknessArmorItem", remap = false)
public abstract class DarknessCloakMixin {
    @Inject(method = "onKeyPacket", at = @At("HEAD"), cancellable = true, require = 0)
    private void mdvlcraft$noFlight(Entity wearer, ItemStack itemStack, int key, CallbackInfo ci) {
        ci.cancel();
    }
}
