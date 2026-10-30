package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.EntityDomainDaburaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.LevelAccessor;

public class DomainExpansionDaburaProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double x_center = 0.0;
         double z_center = 0.0;
         double x_pos = 0.0;
         double y_center = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         double loop_count = 0.0;
         double pitch_s = 0.0;
         double yaw_s = 0.0;
         entity.getPersistentData().m_128347_("select", 48.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") == 34.0) {
            yaw_s = (double)entity.m_146908_();
            pitch_s = (double)entity.m_146909_();
            RotateEntityProcedure.execute(
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               entity
            );
            entity.m_146922_(entity.m_146908_());
            entity.m_146926_(0.0F);
            entity.m_5618_(entity.m_146908_());
            entity.m_5616_(entity.m_146908_());
            entity.f_19859_ = entity.m_146908_();
            entity.f_19860_ = entity.m_146909_();
            if (entity instanceof LivingEntity _entity) {
               _entity.f_20884_ = _entity.m_146908_();
               _entity.f_20886_ = _entity.m_146908_();
            }

            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            dis = MapVariables.get(world).DomainExpansionRadius;
            yaw = (double)(entity.m_146908_() + 90.0F + 180.0F);
            x_center = entity.getPersistentData().m_128459_("x_pos_doma");
            y_center = entity.getPersistentData().m_128459_("y_pos_doma");
            z_center = entity.getPersistentData().m_128459_("z_pos_doma");
            y_pos = y_center;

            for (int index0 = 0; index0 < 4; index0++) {
               x_pos = x_center + Math.cos(Math.toRadians(yaw)) * (dis - 10.0);
               z_pos = z_center + Math.sin(Math.toRadians(yaw)) * (dis - 10.0);
               if (world instanceof ServerLevel _serverLevel) {
                  Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_DOMAIN_DABURA.get())
                     .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                  if (entityinstance != null) {
                     entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                     if (!entityinstance.m_9236_().m_5776_() && entityinstance.m_20194_() != null) {
                        entityinstance.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 entityinstance.m_20182_(),
                                 entityinstance.m_20155_(),
                                 entityinstance.m_9236_() instanceof ServerLevel ? (ServerLevel)entityinstance.m_9236_() : null,
                                 4,
                                 entityinstance.m_7755_().getString(),
                                 entityinstance.m_5446_(),
                                 entityinstance.m_9236_().m_7654_(),
                                 entityinstance
                              ),
                              "data merge entity @s {Invulnerable:1b,NoAI:1b}"
                           );
                     }

                     entityinstance.m_146922_((float)(yaw + 90.0));
                     entityinstance.m_146926_(0.0F);
                     entityinstance.m_5618_(entityinstance.m_146908_());
                     entityinstance.m_5616_(entityinstance.m_146908_());
                     entityinstance.f_19859_ = entityinstance.m_146908_();
                     entityinstance.f_19860_ = entityinstance.m_146909_();
                     if (entityinstance instanceof LivingEntity _entity) {
                        _entity.f_20884_ = _entity.m_146908_();
                        _entity.f_20886_ = _entity.m_146908_();
                     }

                     entityinstance.getPersistentData().m_128347_("NameRanged_ranged", entity.getPersistentData().m_128459_("NameRanged"));
                     entityinstance.getPersistentData().m_128359_("OWNER_UUID", entity.m_20149_());
                     if (loop_count > 0.0) {
                        if (entityinstance instanceof EntityDomainDaburaEntity) {
                           PlayAnimationEntity2Procedure.execute(entityinstance, "idle_side");
                        }

                        if (entityinstance instanceof LivingEntity _livingEntity28
                           && _livingEntity28.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                           _livingEntity28.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(1.0);
                        }
                     } else {
                        if (entityinstance instanceof EntityDomainDaburaEntity) {
                           PlayAnimationEntity2Procedure.execute(entityinstance, "idle");
                        }

                        if (entityinstance instanceof LivingEntity _livingEntity32
                           && _livingEntity32.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                           _livingEntity32.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(1.0);
                        }
                     }

                     _serverLevel.m_7967_(entityinstance);
                  }
               }

               loop_count++;
               yaw += 90.0;
            }

            entity.m_146922_((float)yaw_s);
            entity.m_146926_((float)pitch_s);
            entity.m_5618_(entity.m_146908_());
            entity.m_5616_(entity.m_146908_());
            entity.f_19859_ = entity.m_146908_();
            entity.f_19860_ = entity.m_146909_();
            if (entity instanceof LivingEntity _entity) {
               _entity.f_20884_ = _entity.m_146908_();
               _entity.f_20886_ = _entity.m_146908_();
            }
         }

         PlayAnimationProcedure.execute(world, entity);
      }
   }
}
