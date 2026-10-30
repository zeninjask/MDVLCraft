package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.CrowEntity;
import net.mcreator.jujutsucraft.entity.ItadoriYujiModuloEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;

public class ItadoriDomainActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label66: {
            double old_skill = 0.0;
            double ticks = 0.0;
            range = 0.0;
            double old_cooldown = 0.0;
            double num1 = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label66;
            }

            var10000 = 0.0;
         }

         double var24 = var10000;
         if (var24 % 20.0 <= 5.0 || entity.getPersistentData().m_128471_("StartDomainAttack")) {
            entity.getPersistentData().m_128379_("StartDomainAttack", false);
            double var23 = entity.getPersistentData().m_128459_("skill");
            double var26 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 60.0);
            entity.getPersistentData().m_128347_("skill", (double)(entity instanceof ItadoriYujiModuloEntity ? 106 : 2108));
            entity.getPersistentData().m_128347_("Damage", 15.0 + var24 % 20.0);
            entity.getPersistentData().m_128347_("Range", range);
            entity.getPersistentData().m_128347_("effect", 4.0);
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
            entity.getPersistentData().m_128347_("skill", var23);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var26);
         }

         if (!entity.getPersistentData().m_128471_("Failed")) {
            label54: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var32 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label54;
               }

               var32 = 0;
            }

            if (var32 % 90 == 20) {
               if (entity.getPersistentData().m_128459_("friend_num") == 0.0) {
                  entity.getPersistentData().m_128347_("friend_num", Math.random());
               }

               for (int index0 = 0; index0 < 8; index0++) {
                  if (Math.random() < 0.5) {
                     double var27 = Math.toRadians(Math.random() * 360.0);
                     double var28 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(var27) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     double var29 = entity.getPersistentData().m_128459_("y_pos_doma") + Math.random() * 0.5;
                     double var30 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(var27) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     if (world instanceof ServerLevel) {
                        ServerLevel _serverLevel = (ServerLevel)world;
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.CROW.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var28, var29, var30), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           if (entityinstance instanceof CrowEntity _datEntSetL) {
                              _datEntSetL.m_20088_().m_135381_(CrowEntity.DATA_object, true);
                           }

                           entityinstance.getPersistentData().m_128347_("friend_num", entity.getPersistentData().m_128459_("friend_num"));
                           _serverLevel.m_7967_(entityinstance);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
