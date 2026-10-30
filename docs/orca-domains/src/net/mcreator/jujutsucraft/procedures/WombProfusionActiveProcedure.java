package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class WombProfusionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double var10000;
         label52: {
            double d_bonus = 0.0;
            double old_skill = 0.0;
            double count_A = 0.0;
            double num1 = 0.0;
            double range = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double num2 = 0.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label52;
            }

            var10000 = 0.0;
         }

         double var25 = var10000;
         if (var25 % 80.0 < 15.0) {
            int var10001;
            label45: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10001 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                  break label45;
               }

               var10001 = 0;
            }

            double var28 = MapVariables.get(world).DomainExpansionRadius * (double)(var10001 > 0 ? 9 : 2);
            double var24 = entity.getPersistentData().m_128459_("skill");
            double var29 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
            entity.getPersistentData().m_128347_("y_knockback", -1.0);
            double var23 = var25 % 80.0 + 1.0;
            var25 = 0.0;

            for (int index0 = 0; index0 < 4; index0++) {
               double var27 = var25 % 2.0 == 0.0 ? 1.0 : -1.0;
               double var33 = var25 % 4.0 <= 1.0 ? 1.0 : -1.0;
               double var30 = entity.getPersistentData().m_128459_("x_pos_doma") + var28 * 0.25 * var27;
               double var31 = entity.getPersistentData().m_128459_("y_pos_doma") + var28 * 0.25 - 8.0;
               double var32 = entity.getPersistentData().m_128459_("z_pos_doma") + var28 * 0.25 * var33;
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 100.0);
               entity.getPersistentData().m_128347_("skill", 4110.0);
               entity.getPersistentData().m_128347_("Damage", 15.0 + var23 * 0.5);
               entity.getPersistentData().m_128347_("Range", var28 * 0.5);
               entity.getPersistentData().m_128347_("effect", 13.0);
               entity.getPersistentData().m_128347_("knockback", 0.001);
               entity.getPersistentData().m_128379_("swing", false);
               entity.getPersistentData().m_128379_("attack", false);
               entity.getPersistentData().m_128379_("DomainAttack", true);
               RangeAttackProcedure.execute(world, var30, var31, var32, entity);
               var25++;
            }

            entity.getPersistentData().m_128347_("skill", var24);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var29);
         }
      }
   }
}
