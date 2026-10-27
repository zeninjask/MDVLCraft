package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.ability.ShrineSlashes;
import cursedfate.procedures.SureHitEffectInDomainAreaProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A binder player's closed Malevolent Shrine only cuts while its owner holds the cast key, and its slashes fill the dome
 * (ability/ShrineSlashes.fillDome) instead of Cursed Fate's cleave on each target.
 */
@Mixin(value = SureHitEffectInDomainAreaProcedure.class, remap = false)
public abstract class SureHitEffectInDomainAreaProcedureMixin {
    private static final String TARGET_CLEAVE = "photon:cleaveclosedomain";

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$holdShrineSlashes(LevelAccessor world, double x, double y, double z, Entity domain, CallbackInfo ci) {
        if (domain != null) {
            ServerPlayer owner = ShrineSlashes.owner(domain, "CurrentDomainOwner");
            if (owner != null && "malevolentshrine".equals(owner.getPersistentData().getString("DomainID")) && ShrineSlashes.held(owner)) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/commands/Commands;m_230957_(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"), require = 1)
    private static int mdvlcraft$noTargetCleaves(Commands commands, CommandSourceStack source, String command) {
        return command.contains(TARGET_CLEAVE) ? 0 : commands.performPrefixedCommand(source, command);
    }
}
