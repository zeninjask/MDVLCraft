package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.BarrageProcedure;
import com.mdvlcraft.binder.ability.ProjectionSorcery;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Projection Sorcery's shared command helper, through which its abilities read the cursed energy output and cursed
 * energy: always the highest output, and enough energy, because the binder's wheel charges mana for them instead.
 */
@Mixin(value = BarrageProcedure.class, remap = false)
public abstract class BarrageProcedureMixin {
    @Inject(method = "getCommandResult", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$maxOutput(Entity entity, String command, CallbackInfoReturnable<String> cir) {
        if (command.endsWith("@s Output")) {
            cir.setReturnValue(ProjectionSorcery.MAX_OUTPUT);
        } else if (command.endsWith("@s CursedEnegry")) {
            cir.setReturnValue(ProjectionSorcery.ENOUGH_ENERGY);
        }
    }
}
