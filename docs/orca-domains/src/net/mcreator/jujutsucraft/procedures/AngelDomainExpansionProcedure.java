package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

public class AngelDomainExpansionProcedure {
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
         entity.getPersistentData().m_128347_("select", 28.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") > 0.0
            && entity.getPersistentData().m_128459_("cnt1") > 33.0
            && entity.getPersistentData().m_128459_("cnt1") == 34.0) {
            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(x, y, z),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.bell.use")),
                     SoundSource.NEUTRAL,
                     1.5F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.bell.use")), SoundSource.NEUTRAL, 1.5F, 1.0F, false
                  );
               }
            }

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
            z_pos = entity.getPersistentData().m_128459_("z_pos_doma") + z_pos;
            y_pos = entity.getPersistentData().m_128459_("y_pos_doma");
            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            if (world instanceof ServerLevel _serverLevel) {
               Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_TEMPLE_MAIN.get())
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
                  if (entityinstance instanceof LivingEntity _livingEntity33
                     && _livingEntity33.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                     _livingEntity33.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(2.0);
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
