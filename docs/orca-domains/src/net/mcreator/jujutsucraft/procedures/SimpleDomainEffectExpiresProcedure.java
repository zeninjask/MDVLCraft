package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SimpleDomainEffectExpiresProcedure {
   public static void execute(Entity entity, double amplifier) {
      if (entity != null) {
         if ((!(entity instanceof LivingEntity _livEnt0) || !_livEnt0.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()))
            && amplifier > 0.0
            && entity instanceof LivingEntity _entity
            && !_entity.m_9236_().m_5776_()) {
            _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get(), 60, 0));
         }
      }
   }
}
