package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.modified.UnderworldGate;
import cursedfate.procedures.IncompleteShrineOnTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Underworld Gate's shrine (modified/UnderworldGate) is only the model: Cursed Fate's domain tick (its slashes, sky, music
 * and barrier) never runs for it, and it goes once the gate's time is up (also after a restart, when it was saved).
 */
@Mixin(value = IncompleteShrineOnTickProcedure.class, remap = false)
public abstract class IncompleteShrineOnTickProcedureMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$gateShrine(LevelAccessor world, double x, double y, double z, Entity shrine, CallbackInfo ci) {
        if (shrine != null && shrine.getTags().contains(UnderworldGate.SHRINE_TAG)) {
            if (shrine.tickCount >= UnderworldGate.TICKS) {
                shrine.discard();
            }
            ci.cancel();
        }
    }
}
