package net.mcreator.jujutsucraft.procedures;

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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;

public class TsukumoDomainExpansionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double x_pos = 0.0;
         double pitch = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         dis = MapVariables.get(world).DomainExpansionRadius;
         entity.getPersistentData().m_128347_("select", 9.0);
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

            x_pos = (double)(
               entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(0.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123341_()
                  - entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(dis - 8.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123341_()
            );
            y_pos = (double)(
               entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(0.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123342_()
                  - entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(dis - 8.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123342_()
            );
            z_pos = (double)(
               entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(0.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123343_()
                  - entity.m_9236_()
                     .m_45547_(
                        new ClipContext(
                           entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(dis - 8.0)), Block.OUTLINE, Fluid.NONE, entity
                        )
                     )
                     .m_82425_()
                     .m_123343_()
            );
            x_pos = entity.getPersistentData().m_128459_("x_pos_doma") + x_pos;
            y_pos = entity.getPersistentData().m_128459_("y_pos_doma") + y_pos + 6.0;
            z_pos = entity.getPersistentData().m_128459_("z_pos_doma") + z_pos;
            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            if (world instanceof ServerLevel _serverLevel) {
               Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_JUPITER.get())
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
