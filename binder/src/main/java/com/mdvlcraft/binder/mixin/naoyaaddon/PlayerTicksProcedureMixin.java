package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.PlayerTicksProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phantom Movement has only the Tracing dash: Continuous is off and sneaking no longer switches to it. */
@Mixin(value = PlayerTicksProcedure.class, remap = false)
public abstract class PlayerTicksProcedureMixin {
    @Inject(method = "handleDashMode", at = @At("HEAD"))
    private static void mdvlcraft$tracingOnly(LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        entity.getPersistentData().putBoolean("dashModeContinuous", false);
    }

    @Redirect(method = "handleDashMode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6047_()Z"))
    private static boolean mdvlcraft$noModeToggle(Player player) {
        return false;
    }
}
