package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LogicConfilmDomainProcedure {
   public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity == null) {
         return false;
      } else {
         boolean use = false;
         Entity target_entity = null;
         Entity var16 = entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null;
         if (entity instanceof LivingEntity _livEnt1
               && _livEnt1.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
               && var16 instanceof GojoSatoruEntity
            || var16 instanceof LivingEntity _livEnt3
               && _livEnt3.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
               && entity instanceof GojoSatoruEntity) {
            label177:
            if (var16 instanceof LivingEntity
               && (entity.getPersistentData().m_128459_("cnt_target") > 600.0 || var16.getPersistentData().m_128459_("cnt_target") > 600.0)
               && (!(entity instanceof LivingEntity _livEnt8) || !_livEnt8.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()))
               && (!(var16 instanceof LivingEntity _livEnt9) || !_livEnt9.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get()))
               && entity.getPersistentData().m_128459_("skill") == 0.0
               && var16.getPersistentData().m_128459_("skill") == 0.0) {
               if (var16 instanceof LivingEntity _livEnt12 && _livEnt12.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  int var29;
                  label158: {
                     if (var16 instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                        var29 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                        break label158;
                     }

                     var29 = 0;
                  }

                  if (var29 <= 1000) {
                     break label177;
                  }
               }

               use = true;
            }
         } else {
            int var28;
            label151: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                  var28 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).m_19564_();
                  break label151;
               }

               var28 = 0;
            }

            if (var28 > 0) {
               use = true;
            }
         }

         if (!use) {
            if (var16 instanceof LivingEntity) {
               int var30;
               label142: {
                  if (var16 instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                     var30 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                     break label142;
                  }

                  var30 = 0;
               }

               if (var30 > 1000 || var16.getPersistentData().m_128459_("select") != 0.0 || var16.getPersistentData().m_128459_("skill") % 100.0 == 20.0) {
                  use = true;
               }
            }

            if (entity.getPersistentData().m_128471_("flag_domain")) {
               use = true;
            }
         }

         if (use) {
            int var31;
            label129: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get())) {
                  var31 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).m_19564_();
                  break label129;
               }

               var31 = 0;
            }

            if (var31 >= 2) {
               use = false;
            }

            if (entity instanceof LivingEntity _livEnt21 && _livEnt21.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
               || entity instanceof LivingEntity _livEnt22 && _livEnt22.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
               use = false;
            }
         }

         if (use && GetDistanceProcedure.execute(entity) > MapVariables.get(world).DomainExpansionRadius - 4.0) {
            use = false;
         }

         if (use && entity.getPersistentData().m_128459_("friend_num") != 0.0) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.m_6443_(
               Entity.class, new AABB(_center, _center).m_82400_(MapVariables.get(world).DomainExpansionRadius * 2.0 / 2.0), e -> true
            )) {
               if (entity != entityiterator
                  && entity.getPersistentData().m_128459_("friend_num") == entityiterator.getPersistentData().m_128459_("friend_num")
                  && (
                     entityiterator instanceof LivingEntity _livEnt27 && _livEnt27.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                        || entityiterator.getPersistentData().m_128459_("skill") != 0.0
                           && (
                              entityiterator.getPersistentData().m_128459_("select") != 0.0
                                 || entityiterator.getPersistentData().m_128459_("skill") % 100.0 == 20.0
                           )
                  )) {
                  use = false;
                  break;
               }
            }
         }

         if (use) {
            entity.getPersistentData().m_128379_("flag_domain", false);
         }

         return use;
      }
   }
}
