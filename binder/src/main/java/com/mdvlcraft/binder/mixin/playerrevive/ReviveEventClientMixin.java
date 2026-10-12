package com.mdvlcraft.binder.mixin.playerrevive;

import com.mdvlcraft.binder.client.AbilityKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "team.creative.playerrevive.client.ReviveEventClient", remap = false)
public abstract class ReviveEventClientMixin {
    @Redirect(
        method = {"clientTick", "tick"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyAttack:Lnet/minecraft/client/KeyMapping;", opcode = 180, remap = true),
        require = 0
    )
    private KeyMapping mdvlcraft$giveUpKey(Options options) {
        return AbilityKeys.GIVE_UP;
    }
}
