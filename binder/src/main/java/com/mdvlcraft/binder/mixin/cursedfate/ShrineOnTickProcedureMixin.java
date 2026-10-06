package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.ShrineOnTickProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The open Malevolent Shrine cuts everything around it whenever its "domainnum2" counter reaches 5 (every 5 ticks).
 * While its player owner is not holding Dismantle or Cleave the counter is held at 0, so it never reaches 5; the
 * shrine's music, upkeep and lifetime carry on as usual.
 */
@Mixin(value = ShrineOnTickProcedure.class, remap = false)
public abstract class ShrineOnTickProcedureMixin {
    @Inject(method = "execute", at = @At("HEAD"))
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity shrine, CallbackInfo ci) {
        if (shrine == null) {
            return;
        }
        ServerPlayer owner = ShrineSlashes.owner(shrine, "PlayerUUID");
        if (owner != null && ShrineSlashes.held(owner)) {
            shrine.getPersistentData().putDouble("domainnum2", 0);
        }
    }
}
