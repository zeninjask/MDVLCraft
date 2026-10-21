package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.ShrineOnTickProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The open Malevolent Shrine only cuts while its owner holds the cast key, and only out to 20 blocks (Cursed Fate: 45). */
@Mixin(value = ShrineOnTickProcedure.class, remap = false)
public abstract class ShrineOnTickProcedureMixin {
    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 45.0), require = 1)
    private static double mdvlcraft$cutRadius(double radius) {
        return 20.0;
    }

    @Inject(method = "execute", at = @At("HEAD"))
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity shrine, CallbackInfo ci) {
        if (shrine != null) {
            ServerPlayer owner = ShrineSlashes.owner(shrine, "PlayerUUID");
            if (owner != null && ShrineSlashes.held(owner)) {
                shrine.getPersistentData().putDouble("domainnum2", 0.0);
            }
        }
    }
}
