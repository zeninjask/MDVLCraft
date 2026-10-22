package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.modified.SpellSwings;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iron's melee swing cast animations are not played on players: those spells swing with Epic Fight instead (SpellSwings). */
@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class ClientSpellCastHelperMixin {
    @Inject(method = "animatePlayerStart", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$epicSwing(Player player, ResourceLocation animation, CallbackInfo ci) {
        if (SpellSwings.IRONS_SWINGS.contains(animation)) {
            ci.cancel();
        }
    }
}
