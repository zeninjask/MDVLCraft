package net.mcreator.jujutsucraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DomainExpansionEffectStartedappliedProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         double tick = 0.0;
         double level = 0.0;
         entity.getPersistentData().m_128347_("totalDamage", 0.0);
         entity.getPersistentData().m_128347_("oldHealth", entity instanceof LivingEntity _livEnt ? (double)_livEnt.m_21223_() : -1.0);
      }
   }
}
