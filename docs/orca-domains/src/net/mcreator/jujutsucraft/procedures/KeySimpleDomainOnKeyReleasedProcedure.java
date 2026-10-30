package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class KeySimpleDomainOnKeyReleasedProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity instanceof LivingEntity _entity) {
            _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get());
         }
      }
   }
}
