package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class KurourushiDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double old_skill = 0.0;
         double num1 = 0.0;
         double range = 0.0;
         double old_cooldown = 0.0;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         range = MapVariables.get(world).DomainExpansionRadius * 2.0;
         if (!entity.getPersistentData().m_128471_("Failed")) {
            int var10000;
            label40: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label40;
               }

               var10000 = 0;
            }

            if (var10000 % 45 == 20) {
               old_skill = entity.getPersistentData().m_128459_("skill");
               old_cooldown = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
               entity.getPersistentData().m_128347_("skill", 2306.0);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 100.0);

               for (int index0 = 0; index0 < 4; index0++) {
                  if (Math.random() < 0.5) {
                     num1 = Math.toRadians(Math.random() * 360.0);
                     x_pos = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(num1) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     y_pos = entity.getPersistentData().m_128459_("y_pos_doma") + Math.random() * 0.5;
                     z_pos = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(num1) * (range / 2.0 - 4.0) + Math.random() * 0.5;
                     if (world instanceof ServerLevel) {
                        ServerLevel _serverLevel = (ServerLevel)world;
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.COCKROACHES.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           SetRangedAmmoProcedure.execute(entity, entityinstance);
                           entityinstance.m_146922_((float)(Math.toDegrees(num1) - 90.0));
                           entityinstance.m_146926_(0.0F);
                           entityinstance.m_5618_(entityinstance.m_146908_());
                           entityinstance.m_5616_(entityinstance.m_146908_());
                           entityinstance.f_19859_ = entityinstance.m_146908_();
                           entityinstance.f_19860_ = entityinstance.m_146909_();
                           if (entityinstance instanceof LivingEntity _entity) {
                              _entity.f_20884_ = _entity.m_146908_();
                              _entity.f_20886_ = _entity.m_146908_();
                           }

                           entityinstance.getPersistentData().m_128379_("domain_entity", true);
                           entityinstance.m_7618_(
                              Anchor.EYES,
                              new Vec3(
                                 entity.getPersistentData().m_128459_("x_pos_doma"),
                                 entity.getPersistentData().m_128459_("y_pos_doma"),
                                 entity.getPersistentData().m_128459_("z_pos_doma")
                              )
                           );
                           entityinstance.getPersistentData().m_128347_("x_power", entityinstance.m_20154_().f_82479_);
                           entityinstance.getPersistentData().m_128347_("y_power", entityinstance.m_20154_().f_82480_);
                           entityinstance.getPersistentData().m_128347_("z_power", entityinstance.m_20154_().f_82481_);
                           _serverLevel.m_7967_(entityinstance);
                        }
                     }
                  }
               }

               entity.getPersistentData().m_128347_("skill", old_skill);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", old_cooldown);
            }
         }
      }
   }
}
