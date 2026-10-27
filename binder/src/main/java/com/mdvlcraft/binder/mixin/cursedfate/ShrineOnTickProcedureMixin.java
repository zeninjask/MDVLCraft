package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.ShrineOnTickProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The open Malevolent Shrine (the Master Chef's Malevolent Kitchen) only cuts while its owner holds the cast key, and its
 * slashes (Cursed Fate's cleave effects and sweep sounds) show only while it cuts; it cuts out to 20 blocks (Cursed Fate: 45).
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

    /** The domain-wide slash effects and sweep sounds (once domainnum passes 35) play only while the shrine cuts. */
    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 35.0), require = 1)
    private static double mdvlcraft$slashEffectsWhileCutting(double ticks) {
        return mdvlcraft$slashing ? ticks : Double.MAX_VALUE;
    }

    /** So do its per-target cleave effects (photon commands). */
    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/commands/Commands;m_230957_(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"), require = 1)
    private static int mdvlcraft$cleaveEffectsWhileCutting(Commands commands, CommandSourceStack source, String command) {
        return mdvlcraft$slashing ? commands.performPrefixedCommand(source, command) : 0;
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
