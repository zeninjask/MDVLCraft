package com.mdvlcraft.binder.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** No swirl particles around entities with potion effects (golden apples, beacons, spells). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(method = "tickEffects", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void mdvlcraft$noEffectParticles(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
    }
}
