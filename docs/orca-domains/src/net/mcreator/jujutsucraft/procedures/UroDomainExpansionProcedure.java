package net.mcreator.jujutsucraft.procedures;

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

public class UroDomainExpansionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         Entity entity_a = null;
         double x_pos = 0.0;
         double pitch = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         dis = MapVariables.get(world).DomainExpansionRadius;
         entity.getPersistentData().m_128347_("select", 38.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") > 0.0
            && entity.getPersistentData().m_128459_("cnt1") > 33.0
            && entity.getPersistentData().m_128459_("cnt1") == 34.0) {
            yaw = (double)entity.m_146908_();
            pitch = (double)entity.m_146909_();
            RotateEntityProcedure.execute(
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               entity
            );
            x_pos = entity.getPersistentData().m_128459_("x_pos_doma");
            y_pos = Math.floor(entity.getPersistentData().m_128459_("y_pos_doma"));
            z_pos = entity.getPersistentData().m_128459_("z_pos_doma");
            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            if (world instanceof ServerLevel _serverLevel) {
               Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_DOMAIN_URO.get())
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
                           "data merge entity @s {NoAI:1b,Invulnerable:1b}"
                        );
                  }

                  entityinstance.m_146922_(entity.m_146908_());
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
                  if (entityinstance instanceof LivingEntity _livingEntity26
                     && _livingEntity26.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                     _livingEntity26.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(dis / 22.0 * 0.6);
                  }

                  _serverLevel.m_7967_(entityinstance);
               }
            }

            entity.m_146922_((float)yaw);
            entity.m_146926_((float)pitch);
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
