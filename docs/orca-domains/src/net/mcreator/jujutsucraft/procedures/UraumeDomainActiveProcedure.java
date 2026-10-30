package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class UraumeDomainActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label37: {
            double old_skill = 0.0;
            range = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double tick = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label37;
            }

            var10000 = 0.0;
         }

         double var21 = var10000;
         double var17 = entity.getPersistentData().m_128459_("skill");
         double var19 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
         double var20 = entity.getPersistentData().m_128459_("x_pos_doma");
         double var22 = entity.getPersistentData().m_128459_("y_pos_doma");
         double var23 = entity.getPersistentData().m_128459_("z_pos_doma");
         if (!entity.getPersistentData().m_128471_("Failed")) {
            if (world instanceof ServerLevel _level) {
               _level.m_8767_(
                  (SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_ICE.get(), var20, var22 + 0.0, var23, 20, range * 0.175, 0.1, range * 0.175, 0.05
               );
            }

            if (world instanceof ServerLevel _level) {
               _level.m_8767_(ParticleTypes.f_123815_, var20, var22 + 0.0, var23, 3, range * 0.175, 0.1, range * 0.175, 0.05);
            }
         }

         if (world instanceof ServerLevel _level) {
            _level.m_8767_(
               (SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_ICE.get(),
               var20,
               var22 + range / 2.0 * 0.7,
               var23,
               5,
               range * 0.125,
               0.1,
               range * 0.125,
               0.05
            );
         }

         if (var21 % 40.0 == 0.0 || entity.getPersistentData().m_128471_("StartDomainAttack")) {
            entity.getPersistentData().m_128379_("StartDomainAttack", false);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 25.0);
            entity.getPersistentData().m_128347_("skill", 2405.0);
            entity.getPersistentData().m_128347_("Damage", 5.0);
            entity.getPersistentData().m_128347_("effect", 14.0);
            entity.getPersistentData().m_128347_("Range", range);
            entity.getPersistentData().m_128347_("knockback", 0.0);
            entity.getPersistentData().m_128379_("swing", false);
            entity.getPersistentData().m_128379_("attack", false);
            entity.getPersistentData().m_128379_("DomainAttack", true);
            RangeAttackProcedure.execute(
               world,
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               entity
            );
         }

         entity.getPersistentData().m_128347_("skill", var17);
         entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var19);
      }
   }
}
