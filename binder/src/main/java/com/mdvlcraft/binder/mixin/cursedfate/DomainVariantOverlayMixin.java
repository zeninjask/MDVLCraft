package com.mdvlcraft.binder.mixin.cursedfate;

import cursedfate.network.CursedfateModVariables;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import cursedfate.procedures.ReturnDomainVarientShrineProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cursed Fate's Malevolent Shrine domain picker (its closed / open shrine overlay) also shows for a player whose shrine is
 * cast through the binder (the Master Chef's Malevolent Kitchen): Cursed Fate only shows it for its own Shrine and Vessel
 * techniques. The Calamitous Shrine keeps its own picker.
 */
@Mixin(value = ReturnDomainVarientShrineProcedure.class, remap = false)
public abstract class DomainVariantOverlayMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$binderShrine(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null) {
            PlayerVariables variables = entity.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
            if (variables != null && variables.SelectDomainVarient && !"Calamitous Shrine".equals(variables.Technique)) {
                cir.setReturnValue(true);
            }
        }
    }
}
