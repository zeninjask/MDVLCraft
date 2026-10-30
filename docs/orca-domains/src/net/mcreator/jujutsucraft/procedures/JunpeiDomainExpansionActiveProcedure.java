package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class JunpeiDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label50: {
            double distance = 0.0;
            double old_skill = 0.0;
            double ticks = 0.0;
            range = 0.0;
            double rad1 = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double x_center = 0.0;
            double z_center = 0.0;
            double y_center = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label50;
            }

            var10000 = 0.0;
         }

         double var31 = var10000;
         double var38 = entity.getPersistentData().m_128459_("x_pos_doma");
         double var40 = entity.getPersistentData().m_128459_("y_pos_doma");
         double var39 = entity.getPersistentData().m_128459_("z_pos_doma");
         if (!entity.getPersistentData().m_128471_("Failed")) {
            double var33 = Math.toRadians(90.0 * (var31 % 4.0));
            double var29 = 4.0 * var31 % (range * 0.5 - 5.0);

            for (int index0 = 0; index0 < 4; index0++) {
               for (int index1 = 0; index1 < 72; index1++) {
                  if (!(Math.random() < 0.8)) {
                     var33 += Math.toRadians(10.0 * Math.random());
                     double var35 = var38 + Math.cos(var33) * var29;
                     double var36 = var40 + 0.1;
                     double var37 = var39 + Math.sin(var33) * var29;
                     if (world instanceof ServerLevel _level) {
                        _level.m_8767_(ParticleTypes.f_123771_, var35, var36, var37, 1, 0.5, 1.0, 0.5, 0.05);
                     }
                  }
               }

               var29++;
            }

            if (var31 % 40.0 == 0.0 || entity.getPersistentData().m_128471_("StartDomainAttack")) {
               entity.getPersistentData().m_128379_("StartDomainAttack", false);
               double var30 = entity.getPersistentData().m_128459_("skill");
               double var34 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 100.0);
               entity.getPersistentData().m_128347_("skill", 3505.0);
               entity.getPersistentData().m_128347_("Damage", 3.0);
               entity.getPersistentData().m_128347_("Range", range);
               entity.getPersistentData().m_128347_("effect", 16.0);
               entity.getPersistentData().m_128379_("swing", false);
               entity.getPersistentData().m_128379_("attack", false);
               entity.getPersistentData().m_128379_("onlyLiving", true);
               entity.getPersistentData().m_128379_("DomainAttack", true);
               RangeAttackProcedure.execute(world, var38, var40, var39, entity);
               entity.getPersistentData().m_128347_("skill", var30);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var34);
            }
         }
      }
   }
}
