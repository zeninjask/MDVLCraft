package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.CrowEntity;
import net.mcreator.jujutsucraft.entity.UraumeEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class LogicAttackProcedure {
   public static boolean execute(LevelAccessor world, Entity entity, Entity entityiterator) {
      if (entity == null || entityiterator == null || entity == entityiterator) {
         return false;
      } else if (!(entityiterator instanceof LivingEntity)) {
         return false;
      } else {
         Entity attacker_owner = entity;
         Entity iterator_owner = entityiterator;
         int safeCount = 0;

         for (String aOwnerUUID = entity.getPersistentData().m_128461_("OWNER_UUID"); !aOwnerUUID.isEmpty() && safeCount < 5; safeCount++) {
            Entity found = GetEntityFromUUIDProcedure.execute(world, aOwnerUUID);
            if (!(found instanceof LivingEntity)) {
               break;
            }

            attacker_owner = found;
            aOwnerUUID = found.getPersistentData().m_128461_("OWNER_UUID");
         }

         safeCount = 0;

         for (String iOwnerUUID = entityiterator.getPersistentData().m_128461_("OWNER_UUID"); !iOwnerUUID.isEmpty() && safeCount < 5; safeCount++) {
            Entity found = GetEntityFromUUIDProcedure.execute(world, iOwnerUUID);
            if (!(found instanceof LivingEntity)) {
               break;
            }

            iterator_owner = found;
            iOwnerUUID = found.getPersistentData().m_128461_("OWNER_UUID");
         }

         if (entity.getPersistentData().m_128471_("onlyLiving")
            && entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))) {
            return false;
         } else if (entity.getPersistentData().m_128471_("onlyRanged")
            && !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))) {
            return false;
         } else if (entityiterator.m_20147_()) {
            return false;
         } else {
            if (entityiterator instanceof Player p && (p.m_150110_().f_35937_ || p.m_5833_())) {
               return false;
            }

            if ((
                  !entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))
                     || entity.m_146895_() != entityiterator
               )
               && (
                  !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))
                     || entityiterator.m_146895_() != entity
               )) {
               boolean logic_attack = true;
               boolean pTarget = attacker_owner instanceof Player;
               boolean pIterator = iterator_owner instanceof Player;
               if (pTarget && pIterator) {
                  if (!world.m_6106_().m_5470_().m_46207_(JujutsucraftModGameRules.JUJUTSUPVP)) {
                     logic_attack = false;
                  }
               } else if (!attacker_owner.getPersistentData().m_128471_("JujutsuSorcerer") && !entity.getPersistentData().m_128471_("JujutsuSorcerer")
                  || !iterator_owner.getPersistentData().m_128471_("JujutsuSorcerer") && !entityiterator.getPersistentData().m_128471_("JujutsuSorcerer")) {
                  if (!attacker_owner.getPersistentData().m_128471_("CursedSpirit") && !entity.getPersistentData().m_128471_("CursedSpirit")
                     || !iterator_owner.getPersistentData().m_128471_("CursedSpirit") && !entityiterator.getPersistentData().m_128471_("CursedSpirit")) {
                     if ((attacker_owner.getPersistentData().m_128471_("CurseUser") || entity.getPersistentData().m_128471_("CurseUser"))
                        && (iterator_owner.getPersistentData().m_128471_("CurseUser") || entityiterator.getPersistentData().m_128471_("CurseUser"))) {
                        logic_attack = false;
                     }
                  } else {
                     logic_attack = false;
                  }
               } else {
                  logic_attack = false;
               }

               if (attacker_owner instanceof LivingEntity le && le.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                  || entity instanceof LivingEntity le2 && le2.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                  || iterator_owner instanceof LivingEntity le3 && le3.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                  || entityiterator instanceof LivingEntity le4 && le4.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                  logic_attack = true;
               }

               if (logic_attack) {
                  if (attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian")))) {
                     if (iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian")))) {
                        logic_attack = false;
                     }

                     if (attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian_rumel")))
                        && iterator_owner.getPersistentData().m_128471_("CursedSpirit")) {
                        logic_attack = false;
                     }
                  }

                  if (iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian")))) {
                     if (attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian")))) {
                        logic_attack = false;
                     }

                     if (iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:simurian_rumel")))
                        && attacker_owner.getPersistentData().m_128471_("CursedSpirit")) {
                        logic_attack = false;
                     }
                  }

                  if (attacker_owner instanceof UraumeEntity
                     && (
                        ReturnInsideItemProcedure.execute(iterator_owner).m_41720_() == JujutsucraftModItems.SUKUNA_FINGER.get()
                           || iterator_owner instanceof LivingEntity le && le.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                     )) {
                     logic_attack = false;
                  }

                  if (iterator_owner instanceof UraumeEntity
                     && (
                        ReturnInsideItemProcedure.execute(attacker_owner).m_41720_() == JujutsucraftModItems.SUKUNA_FINGER.get()
                           || attacker_owner instanceof LivingEntity le && le.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                     )) {
                     logic_attack = false;
                  }

                  if ((
                           attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_1")))
                              || attacker_owner instanceof Player && attacker_owner.getPersistentData().m_128471_("CurseUser")
                        )
                        && (
                           iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_1")))
                              || iterator_owner instanceof Player && iterator_owner.getPersistentData().m_128471_("CurseUser")
                        )
                     || attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_2")))
                        && iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_2")))
                     || attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_3")))
                        && iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_3")))
                     || attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_4")))
                        && iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_4")))
                     || attacker_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_5")))
                        && iterator_owner.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:group_5")))) {
                     logic_attack = false;
                  }
               }

               if (!logic_attack) {
                  if (pTarget
                     && entityiterator instanceof Mob m
                     && m.m_5448_() instanceof LivingEntity
                     && entityiterator.getPersistentData().m_128459_("cnt_target") > 6.0
                     && (m.m_5448_() instanceof Player || m.m_5448_().getPersistentData().m_128471_("Player"))) {
                     logic_attack = true;
                  }

                  if (pIterator
                     && entity instanceof Mob m
                     && m.m_5448_() instanceof LivingEntity
                     && entity.getPersistentData().m_128459_("cnt_target") > 6.0
                     && (m.m_5448_() instanceof Player || m.m_5448_().getPersistentData().m_128471_("Player"))) {
                     logic_attack = true;
                  }
               }

               if (entity instanceof CrowEntity && entityiterator instanceof CrowEntity) {
                  logic_attack = false;
               }

               boolean targeting;
               label491: {
                  targeting = false;
                  if (entity instanceof Mob m && m.m_5448_() == entityiterator && entity.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                     break label491;
                  }

                  if (entityiterator instanceof Mob m && m.m_5448_() == entity && entityiterator.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                     break label491;
                  }

                  if (attacker_owner instanceof Mob m && m.m_5448_() == entityiterator && attacker_owner.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                     break label491;
                  }

                  if (iterator_owner instanceof Mob m && m.m_5448_() == entity && iterator_owner.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                     break label491;
                  }

                  if (attacker_owner instanceof Mob m && m.m_5448_() == iterator_owner && attacker_owner.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                     break label491;
                  }

                  if (iterator_owner instanceof Mob m && m.m_5448_() == attacker_owner && iterator_owner.getPersistentData().m_128459_("cnt_target") > 6.0) {
                     targeting = true;
                  }
               }

               if (targeting) {
                  logic_attack = true;
               }

               if (attacker_owner == iterator_owner) {
                  logic_attack = false;
               }

               if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:ten_shadows_technique")))
                     && !entity.getPersistentData().m_128471_("Ambush")
                  || entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:ten_shadows_technique")))
                     && !entityiterator.getPersistentData().m_128471_("Ambush")) {
                  logic_attack = true;
               }

               if (entity.getPersistentData().m_128459_("friend_num") != 0.0
                  && entity.getPersistentData().m_128459_("friend_num") == entityiterator.getPersistentData().m_128459_("friend_num")) {
                  logic_attack = false;
               }

               double myRanged = entity.getPersistentData().m_128459_("NameRanged_ranged");
               double myName = entity.getPersistentData().m_128459_("NameRanged");
               double targetName = entityiterator.getPersistentData().m_128459_("NameRanged");
               double targetRanged = entityiterator.getPersistentData().m_128459_("NameRanged_ranged");
               if (myRanged != 0.0) {
                  if (myRanged == targetName || myRanged == targetRanged) {
                     logic_attack = false;
                  }

                  if (entity.getPersistentData().m_128471_("betrayal")) {
                     if (!iterator_owner.m_20149_().equals(entity.m_20149_()) && !entity.m_20149_().equals(iterator_owner.m_20149_())) {
                        if (myRanged != targetRanged) {
                           return true;
                        }
                     } else {
                        logic_attack = false;
                     }
                  }
               }

               if (myName != 0.0 && (myName == targetName || myName == targetRanged)) {
                  logic_attack = false;
               }

               if (entityiterator instanceof LivingEntity le
                  && le.m_21023_((MobEffect)JujutsucraftModMobEffects.PRAYER_SONG.get())
                  && le.m_21023_((MobEffect)JujutsucraftModMobEffects.GUARD.get())
                  && le.m_21124_((MobEffect)JujutsucraftModMobEffects.GUARD.get()).m_19564_() > 0) {
                  double s = entity.getPersistentData().m_128459_("skill");
                  if ((!(s >= 305.0) || !(s <= 310.0)) && s != 205.0 && s != 705.0) {
                     logic_attack = false;
                  }
               }

               if (!logic_attack) {
                  return false;
               } else if (entity.getPersistentData().m_128471_("DomainAttack") && !LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                  return false;
               } else {
                  double targetType = entity.getPersistentData().m_128459_("target_type");
                  if (targetType != 0.0) {
                     double dx = entityiterator.m_20184_().m_7096_();
                     double dy = entityiterator.m_20184_().m_7098_();
                     double dz = entityiterator.m_20184_().m_7094_();
                     double vSq = dx * dx + dy * dy + dz * dz;
                     if (targetType == 1.0) {
                        if (vSq <= 1.0) {
                           return false;
                        }
                     } else if (targetType == 2.0 && vSq < 0.01 && !entityiterator.m_20142_()) {
                        return false;
                     }
                  }

                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }
}
