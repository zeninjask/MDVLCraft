package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.ability.ManaRegen;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mana regeneration waits ManaRegen.PAUSE_TICKS after the last spend, and restores half of Iron's amount. */
@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {
    @Inject(method = "regenPlayerMana", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$pauseAfterSpend(ServerPlayer player, MagicData magicData, CallbackInfoReturnable<Boolean> cir) {
        if (ManaRegen.paused(player)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "regenPlayerMana", at = @At("STORE"), ordinal = 2)
    private float mdvlcraft$halfRegen(float increment) {
        return increment * ManaRegen.REGEN_MULTIPLIER;
    }
}
