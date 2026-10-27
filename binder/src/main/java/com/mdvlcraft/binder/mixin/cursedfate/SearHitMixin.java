package com.mdvlcraft.binder.mixin.cursedfate;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sear's flame (Cursed Fate's Fuga, and Divine Flame inside the chef's own open shrine) where it lands: everything within
 * 4.5 blocks is hit and set alight for 15 s. Party members and other allies of the caster (Entity.isAlliedTo, which asks
 * Parties, mixin/parties/EntityAllianceMixin) are neither hit nor set alight; everything else takes twice the damage.
 */
@Mixin(targets = {
    "cursedfate.procedures.FugaProjectileHitsBlockProcedure",
    "cursedfate.procedures.FugaProjectileHitsEntityProcedure",
    "cursedfate.procedures.DivineProjectileHitsBlockProcedure",
    "cursedfate.procedures.DivineProjectileOnHitEntityProcedure"
}, remap = false)
public abstract class SearHitMixin {
    @Unique
    private static final float SEAR_DAMAGE = 2.0F;
    /** The ally whose hit was just skipped: its fire is skipped too (each hit is followed by its setSecondsOnFire). */
    @Unique
    private static final ThreadLocal<Entity> mdvlcraft$spared = new ThreadLocal<>();

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 2)
    private static boolean mdvlcraft$searHit(Entity target, DamageSource source, float amount) {
        Entity caster = source.getEntity();
        if (caster != null && target.isAlliedTo(caster)) {
            mdvlcraft$spared.set(target);
            return false;
        }
        mdvlcraft$spared.remove();
        return target.hurt(source, amount * SEAR_DAMAGE);
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20254_(I)V"), require = 2)
    private static void mdvlcraft$searFire(Entity target, int seconds) {
        if (mdvlcraft$spared.get() != target) {
            target.setSecondsOnFire(seconds);
        }
    }
}
