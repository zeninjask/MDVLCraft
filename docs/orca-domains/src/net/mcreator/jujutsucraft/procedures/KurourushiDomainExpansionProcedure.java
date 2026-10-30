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
import net.minecraft.world.level.LevelAccessor;

public class KurourushiDomainExpansionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double x_center = 0.0;
         double z_center = 0.0;
         double num1 = 0.0;
         double x_pos = 0.0;
         double y_center = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double dis = 0.0;
         double num2 = 0.0;
         dis = MapVariables.get(world).DomainExpansionRadius;
         x_center = entity.getPersistentData().m_128459_("x_pos_doma");
         y_center = entity.getPersistentData().m_128459_("y_pos_doma");
         z_center = entity.getPersistentData().m_128459_("z_pos_doma");
         entity.getPersistentData().m_128347_("select", 23.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") > 0.0 && entity.getPersistentData().m_128459_("cnt1") == 34.0) {
            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            for (int index0 = 0; index0 < 16; index0++) {
               num1 = Math.toRadians(Math.random() * 720.0);

               for (int index1 = 0; index1 < 32; index1++) {
                  num2 = Math.random();
                  if (num2 > 0.5) {
                     num2 *= dis - 3.0;
                     break;
                  }
               }

               x_pos = x_center + Math.sin(num1) * num2;
               y_pos = y_center + 1.0;
               z_pos = z_center + Math.cos(num1) * num2;

               for (int index2 = 0; index2 < 100; index2++) {
                  if (world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()) {
                     y_pos++;
                  } else if (!world.m_8055_(BlockPos.m_274561_(x_pos, y_pos - 1.0, z_pos)).m_60815_()) {
                     y_pos--;
                  } else if (!world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()
                     && world.m_8055_(BlockPos.m_274561_(x_pos, y_pos - 1.0, z_pos)).m_60815_()) {
                     y_pos = Math.floor(y_pos);
                     break;
                  }
               }

               if (world instanceof ServerLevel _serverLevel) {
                  Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_COCKROACH_EGGS.get())
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

                     entityinstance.m_146922_((float)(Math.random() * 360.0));
                     entityinstance.m_146926_((float)((Math.random() - 0.5) * 30.0));
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
            }
         }

         PlayAnimationProcedure.execute(world, entity);
      }
   }
}
