package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.ShrineOnTickProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The open Malevolent Shrine only cuts, and only shows its slashes (the cleave effects and sweep sounds Cursed Fate plays
 * once it has stood 35 ticks), while its owner holds the cast key; it cuts only out to 20 blocks (Cursed Fate: 45).
 */
@Mixin(value = ShrineOnTickProcedure.class, remap = false)
public abstract class ShrineOnTickProcedureMixin {
    /** Whether the shrine being ticked is cutting (always, for one without a player owner); set at the head of each tick. */
    @Unique
    private static boolean mdvlcraft$slashing;

    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 45.0), require = 1)
    private static double mdvlcraft$cutRadius(double radius) {
        return 20.0;
    }

    /** The slash effects start once domainnum passes 35; never while the shrine isn't cutting. */
    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 35.0), require = 1)
    private static double mdvlcraft$slashEffectsAfter(double ticks) {
        return mdvlcraft$slashing ? ticks : Double.MAX_VALUE;
    }

    @Inject(method = "execute", at = @At("HEAD"))
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity shrine, CallbackInfo ci) {
        ServerPlayer owner = shrine == null ? null : ShrineSlashes.owner(shrine, "PlayerUUID");
        mdvlcraft$slashing = owner == null || !ShrineSlashes.held(owner);
        if (!mdvlcraft$slashing) {
            shrine.getPersistentData().putDouble("domainnum2", 0.0);
        }
    }
}
