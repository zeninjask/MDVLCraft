package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.CrowEntity;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class MeimeiDomainActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double num1 = 0.0;
         double range = 0.0;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         range = MapVariables.get(world).DomainExpansionRadius * 2.0;
         if (!entity.getPersistentData().m_128471_("Failed")) {
            int var10000;
            label58: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label58;
               }

               var10000 = 0;
            }

            if (var10000 % 90 == 20) {
               if (entity.getPersistentData().m_128459_("friend_num") == 0.0) {
                  entity.getPersistentData().m_128347_("friend_num", Math.random());
               }

               for (int index0 = 0; index0 < 8; index0++) {
                  if (Math.random() < 0.5) {
                     num1 = Math.toRadians(Math.random() * 360.0);
                     x_pos = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(num1) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     y_pos = entity.getPersistentData().m_128459_("y_pos_doma") + Math.random() * 0.5;
                     z_pos = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(num1) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     if (world instanceof ServerLevel) {
                        ServerLevel _serverLevel = (ServerLevel)world;
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.CROW.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           if (entityinstance instanceof CrowEntity _datEntSetL) {
                              _datEntSetL.m_20088_().m_135381_(CrowEntity.DATA_object, true);
                           }

                           if (entityinstance instanceof TamableAnimal) {
                              TamableAnimal _toTame = (TamableAnimal)entityinstance;
                              if (entity instanceof Player _owner) {
                                 _toTame.m_21828_(_owner);
                              }
                           }

                           entityinstance.getPersistentData().m_128359_("OWNER_UUID", entity.m_20149_());
                           entityinstance.getPersistentData().m_128347_("friend_num", entity.getPersistentData().m_128459_("friend_num"));
                           entityinstance.getPersistentData().m_128379_("Player", entity instanceof Player || entity.getPersistentData().m_128471_("Player"));
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
