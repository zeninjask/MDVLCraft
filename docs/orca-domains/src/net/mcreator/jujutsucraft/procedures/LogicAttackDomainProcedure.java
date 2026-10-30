package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.DomainExpansionEntityEntity;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class LogicAttackDomainProcedure {
   public static boolean execute(LevelAccessor world, Entity entity, Entity entityiterator) {
      if (world.m_5776_()) {
         return false;
      } else if (entity == null || entityiterator == null) {
         return false;
      } else if (!(entityiterator instanceof LivingEntity target)) {
         return false;
      } else {
         if (target instanceof Player p && (p.m_150110_().f_35937_ || p.m_5833_())) {
            return false;
         }

         if (target instanceof DomainExpansionEntityEntity) {
            return false;
         } else {
            boolean isZeroCursePower = false;
            if (target instanceof Player pTarget) {
               isZeroCursePower = ((JujutsucraftModVariables.PlayerVariables)pTarget.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new JujutsucraftModVariables.PlayerVariables()))
                     .PlayerCursePowerFormer
                  == 0.0;
            } else {
               isZeroCursePower = target.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:no_curse_power")));
            }

            if (isZeroCursePower) {
               MobEffectInstance domainEffect = entity instanceof LivingEntity le
                  ? le.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                  : null;
               if (domainEffect == null || domainEffect.m_19564_() == 0) {
                  return false;
               }
            }

            if (!target.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) && target.getPersistentData().m_128459_("select") == 0.0) {
               MobEffectInstance attackerDomain = entity instanceof LivingEntity le
                  ? le.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                  : null;
               if (target.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())
                  && LogicSimpleDomainProcedure.execute()
                  && attackerDomain != null
                  && attackerDomain.m_19564_() >= 0) {
                  return false;
               } else if (!target.m_21023_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                  return false;
               } else if (entity.getPersistentData().m_128471_("Failed") || entity.getPersistentData().m_128471_("Cover")) {
                  return false;
               } else if (entity.getPersistentData().m_128471_("onlyLiving")
                  && target.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))) {
                  return false;
               } else {
                  double friendNum = entity.getPersistentData().m_128459_("friend_num");
                  if (friendNum != 0.0 && friendNum == target.getPersistentData().m_128459_("friend_num")) {
                     return false;
                  } else {
                     double myRanged = entity.getPersistentData().m_128459_("NameRanged_ranged");
                     double myName = entity.getPersistentData().m_128459_("NameRanged");
                     double targetName = target.getPersistentData().m_128459_("NameRanged");
                     double targetRanged = target.getPersistentData().m_128459_("NameRanged_ranged");
                     if (myRanged != 0.0 && (myRanged == targetName || myRanged == targetRanged)) {
                        return false;
                     } else if (myName == 0.0 || myName != targetName && myName != targetRanged) {
                        ItemStack helmet = target.m_6844_(EquipmentSlot.HEAD);
                        if (helmet.m_41720_() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                           || helmet.m_41720_() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()) {
                           boolean isMahoraga = false;
                           if (!(target instanceof Player pTarget)) {
                              isMahoraga = target instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
                           } else {
                              JujutsucraftModVariables.PlayerVariables pVars = (JujutsucraftModVariables.PlayerVariables)pTarget.getCapability(
                                    JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null
                                 )
                                 .orElse(new JujutsucraftModVariables.PlayerVariables());
                              isMahoraga = pVars.PlayerCurseTechnique == 16.0 || pVars.PlayerCurseTechnique2 == 16.0;
                           }

                           if (isMahoraga && helmet.m_41784_().m_128459_("domain" + Math.round(entity.getPersistentData().m_128459_("skill_domain"))) >= 100.0) {
                              return false;
                           }
                        }

                        if (entity instanceof OkkotsuYutaCullingGameEntity) {
                           boolean old_domainAttack = entity.getPersistentData().m_128471_("DomainAttack");
                           entity.getPersistentData().m_128379_("DomainAttack", false);
                           boolean canAttack = LogicAttackProcedure.execute(world, entity, entityiterator);
                           entity.getPersistentData().m_128379_("DomainAttack", old_domainAttack);
                           if (!canAttack) {
                              return false;
                           }
                        }

                        return true;
                     } else {
                        return false;
                     }
                  }
               }
            } else {
               return false;
            }
         }
      }
   }
}
