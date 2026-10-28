package com.mdvlcraft.binder.mixin.cursedfate;

import cursedfate.procedures.FlowAuraOnTickProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cursed Fate's flow aura is off in the pack: it never starts (Flow is cleared on login, ability/AbilityEvents). */
@Mixin(value = FlowAuraOnTickProcedure.class, remap = false)
public abstract class FlowAuraOnTickProcedureMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$noAura(Entity entity, CallbackInfo ci) {
        ci.cancel();
    }
}
