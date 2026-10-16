package com.mdvlcraft.binder.mixin.csw;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Interlace Spellweaves no longer tells the player "Your magic comprehension deepens" the first time they cast a spell. */
@Mixin(targets = "org.xszb.interlace_spellweaves.capability.SpellCapabilityHandler", remap = false)
public abstract class SpellCapabilityHandlerMixin {
    @Redirect(method = "onPlayerCast",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_213846_(Lnet/minecraft/network/chat/Component;)V"))
    private static void mdvlcraft$quiet(Player player, Component message) {
    }
}
