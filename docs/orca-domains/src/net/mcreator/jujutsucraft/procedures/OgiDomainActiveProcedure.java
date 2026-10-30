package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class OgiDomainActiveProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label56: {
            double distance = 0.0;
            double old_skill = 0.0;
            range = 0.0;
            double rad1 = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double tick = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label56;
            }

            var10000 = 0.0;
         }

         double var35 = var10000;
         if (entity.getPersistentData().m_128471_("Failed")) {
            if (entity.m_20096_() && world instanceof ServerLevel _level) {
               _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get(), x, y, z, 20, 1.0, 0.0, 1.0, 0.05);
            }
         } else {
            double var32 = Math.toRadians(90.0 * (var35 % 4.0));
            double var29 = 4.0 * var35 % (range * 0.5 - 5.0);

            for (int index0 = 0; index0 < 5; index0++) {
               for (int index1 = 0; index1 < 72; index1++) {
                  if (!(Math.random() < 0.9)) {
                     var32 += Math.toRadians(10.0 * Math.random());
                     double var34 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.cos(var32) * var29;
                     double var36 = entity.getPersistentData().m_128459_("y_pos_doma") + 0.1;
                     double var37 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.sin(var32) * var29;
                     if (world instanceof ServerLevel _level) {
                        _level.m_8767_(
                           (SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_BLUE.get(), var34, var36, var37, 1, 0.2, 0.1, 0.2, 0.05
                        );
                     }
                  }
               }

               var29++;
            }

            if (var35 % 20.0 == 0.0 || entity.getPersistentData().m_128471_("StartDomainAttack")) {
               entity.getPersistentData().m_128379_("StartDomainAttack", false);
               double var30 = entity.getPersistentData().m_128459_("skill");
               double var33 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 60.0);
               entity.getPersistentData().m_128347_("skill", 406.0);
               entity.getPersistentData().m_128347_("Damage", 9.0);
               entity.getPersistentData().m_128347_("Range", range);
               entity.getPersistentData().m_128347_("effect", 6.0);
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
               entity.getPersistentData().m_128347_("skill", var30);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var33);
            }
         }
      }
   }
}
