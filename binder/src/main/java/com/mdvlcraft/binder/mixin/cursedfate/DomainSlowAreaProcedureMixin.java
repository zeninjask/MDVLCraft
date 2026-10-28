package com.mdvlcraft.binder.mixin.cursedfate;

import cursedfate.procedures.DomainSlowAreaProcedure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opening a domain (the Malevolent Shrine among them) slows everything within 20 blocks of the caster. It no longer slows
 * the caster's party members or anything else allied to them (Entity.isAlliedTo, which asks Parties); the caster and
 * everyone else are slowed as before. Cursed Fate passes only the caster's position, so the caster is the player there.
 */
@Mixin(value = DomainSlowAreaProcedure.class, remap = false)
public abstract class DomainSlowAreaProcedureMixin {
    @Unique
    private static Player mdvlcraft$caster;

    @Inject(method = "execute", at = @At("HEAD"))
    private static void mdvlcraft$findCaster(LevelAccessor world, double x, double y, double z, CallbackInfo ci) {
        mdvlcraft$caster = null;
        double best = 2.0 * 2.0;
        for (Player player : world.getEntitiesOfClass(Player.class, new AABB(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2))) {
            double distance = player.distanceToSqr(x, y, z);
            if (distance < best) {
                best = distance;
                mdvlcraft$caster = player;
            }
        }
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 1)
    private static boolean mdvlcraft$spareAllies(LivingEntity target, MobEffectInstance effect) {
        Player caster = mdvlcraft$caster;
        if (caster != null && target != caster && target.isAlliedTo(caster)) {
            return false;
        }
        return target.addEffect(effect);
    }
}
