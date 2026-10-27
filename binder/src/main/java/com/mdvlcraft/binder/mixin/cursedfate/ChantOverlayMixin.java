package com.mdvlcraft.binder.mixin.cursedfate;

import cursedfate.procedures.ChantOverlayDisplayOverlayIngameProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cursed Fate's chant lines in the middle of the screen never show: skills are called out by name instead (ability/SkillCallout). */
@Mixin(value = ChantOverlayDisplayOverlayIngameProcedure.class, remap = false)
public abstract class ChantOverlayMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noChantLines(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
