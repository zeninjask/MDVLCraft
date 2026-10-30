package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TsukumoDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double rad;
         double z_center;
         double range;
         double y_center;
         double x_center;
         int var10000;
         label96: {
            boolean logic_a = false;
            z_center = 0.0;
            range = 0.0;
            double x_pos = 0.0;
            y_center = 0.0;
            double z_pos = 0.0;
            double yaw = 0.0;
            double dis = 0.0;
            double x_knockback = 0.0;
            x_center = 0.0;
            rad = 0.0;
            double rad_now = 0.0;
            double z_knockback = 0.0;
            double y_pos = 0.0;
            double y_knockback = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            x_center = entity.getPersistentData().m_128459_("x_pos_doma");
            y_center = entity.getPersistentData().m_128459_("y_pos_doma");
            z_center = entity.getPersistentData().m_128459_("z_pos_doma");
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label96;
            }

            var10000 = 0;
         }

         if (var10000 % 5 == 0 && !entity.getPersistentData().m_128471_("Failed")) {
            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.STAR_RAGE.get(), 10, 11, true, true));
            }

            Vec3 _center = new Vec3(x_center, y_center, z_center);

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
               if (entity != entityiterator && LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 20, 6, false, false));
                     }
                  }

                  double var46 = entityiterator.m_20185_() - entityiterator.m_20185_();
                  double var55 = entityiterator.m_20186_() - 5.0 - entityiterator.m_20186_();
                  double var51 = entityiterator.m_20189_() - entityiterator.m_20189_();
                  double var44 = Math.sqrt(Math.pow(var46, 2.0) + Math.pow(var55, 2.0) + Math.pow(var51, 2.0));
                  var46 = var46 / var44 * 3.0;
                  var55 = var55 / var44 * 3.0;
                  var51 = var51 / var44 * 3.0;
                  if (!(var46 * 1.1 < entityiterator.m_20184_().m_7096_()) && !(var46 * 0.9 > entityiterator.m_20184_().m_7096_())) {
                     var46 = entityiterator.m_20184_().m_7096_();
                  } else {
                     var46 = entityiterator.m_20184_().m_7096_() + var46 * 0.1;
                  }

                  if (!(var55 * 1.1 < entityiterator.m_20184_().m_7098_()) && !(var55 * 0.9 > entityiterator.m_20184_().m_7098_())) {
                     var55 = entityiterator.m_20184_().m_7098_();
                  } else {
                     var55 = entityiterator.m_20184_().m_7098_() + var55 * 0.1;
                  }

                  if (!(var51 * 1.1 < entityiterator.m_20184_().m_7094_()) && !(var51 * 0.9 > entityiterator.m_20184_().m_7094_())) {
                     var51 = entityiterator.m_20184_().m_7094_();
                  } else {
                     var51 = entityiterator.m_20184_().m_7094_() + var51 * 0.1;
                  }

                  entityiterator.m_20256_(new Vec3(var46, var55, var51));
               }
            }
         }

         double var45 = range * 0.5 - 4.0;
         double var43 = Math.toRadians(Math.random() * 360.0);
         double var50 = Math.toRadians((Math.random() - 0.5) * 180.0);
         boolean var37 = Math.random() < 0.5;

         for (int index0 = 0; index0 < 72; index0++) {
            if (Math.random() < 0.5) {
               double var40;
               double var42;
               double var54;
               if (var37) {
                  var40 = x_center + Math.cos(rad) * (Math.cos(var50) + Math.abs(Math.sin(var43) * Math.sin(var50))) * var45;
                  var54 = y_center + Math.sin(rad) * Math.sin(var50) * -1.0 * var45;
                  var42 = z_center + Math.sin(rad) * (Math.cos(var50) + Math.abs(Math.cos(var43) * Math.sin(var50))) * var45;
               } else {
                  var40 = x_center + Math.cos(rad) * Math.cos(var50) * var45;
                  var54 = y_center + Math.sin(rad) * var45;
                  var42 = z_center + Math.cos(rad) * Math.sin(var50) * var45;
               }

               if (world instanceof ServerLevel _level) {
                  _level.m_8767_(ParticleTypes.f_123760_, var40, var54, var42, 1, 0.5, 0.5, 0.5, 0.0);
               }
            }

            rad += Math.toRadians(10.0 * Math.random());
         }
      }
   }
}
