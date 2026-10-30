package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AIDomainLogicProcedure {
   public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity == null) {
         return false;
      } else {
         boolean cancel = false;
         Entity target_entity = null;
         if (Math.random()
            > (double)(
                  1.0F
                     - Math.max(entity instanceof LivingEntity _livEntx ? _livEntx.m_21223_() : -1.0F, 1.0F)
                        / Math.max(entity instanceof LivingEntity _livEnt ? _livEnt.m_21233_() : -1.0F, 1.0F)
               )
               * 0.75) {
            cancel = true;
            Entity var15 = entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null;
            if (var15 instanceof LivingEntity _livEnt3 && _livEnt3.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
               || var15 instanceof LivingEntity _livEnt4 && _livEnt4.m_21023_((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
               cancel = false;
            }

            if (var15.getPersistentData().m_128459_("skill") % 100.0 == 20.0) {
               cancel = false;
            }

            int var26;
            label115: {
               if (entity instanceof LivingEntity _livEntxx && _livEntxx.m_21023_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                  var26 = _livEntxx.m_21124_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).m_19564_();
                  break label115;
               }

               var26 = 0;
            }

            if (var26 > 0) {
               cancel = false;
            }
         }

         if (!cancel) {
            if (entity.getPersistentData().m_128459_("cnt_target") < 200.0) {
               cancel = true;
            }

            int var27;
            label107: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get())) {
                  var27 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).m_19564_();
                  break label107;
               }

               var27 = 0;
            }

            if (var27 >= 2) {
               cancel = true;
            }

            if (entity instanceof LivingEntity _livEnt9 && _livEnt9.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
               || entity instanceof LivingEntity _livEnt10 && _livEnt10.m_21023_((MobEffect)JujutsucraftModMobEffects.UNSTABLE.get())) {
               cancel = true;
            }
         }

         if (!cancel && GetDistanceProcedure.execute(entity) > MapVariables.get(world).DomainExpansionRadius) {
            cancel = true;
         }

         if (!cancel && entity.getPersistentData().m_128459_("friend_num") != 0.0) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.m_6443_(
               Entity.class, new AABB(_center, _center).m_82400_(MapVariables.get(world).DomainExpansionRadius * 2.0 / 2.0), e -> true
            )) {
               if (entity != entityiterator
                  && entity.getPersistentData().m_128459_("friend_num") == entityiterator.getPersistentData().m_128459_("friend_num")
                  && (
                     entityiterator instanceof LivingEntity _livEnt15 && _livEnt15.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                        || entityiterator.getPersistentData().m_128459_("select") != 0.0
                        || entityiterator.getPersistentData().m_128459_("skill") % 100.0 == 20.0
                  )) {
                  cancel = true;
                  break;
               }
            }
         }

         return cancel;
      }
   }
}
