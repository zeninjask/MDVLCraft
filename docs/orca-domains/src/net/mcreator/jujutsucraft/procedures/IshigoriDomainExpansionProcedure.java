package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.EntityDomainIshigoriEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class IshigoriDomainExpansionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         Entity entity_a = null;
         double num1 = 0.0;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double num2 = 0.0;
         double dis = 0.0;
         double x_center = 0.0;
         double z_center = 0.0;
         double y_center = 0.0;
         double distance = 0.0;
         double pitch = 0.0;
         double yaw = 0.0;
         dis = MapVariables.get(world).DomainExpansionRadius;
         x_center = entity.getPersistentData().m_128459_("x_pos_doma");
         y_center = entity.getPersistentData().m_128459_("y_pos_doma");
         z_center = entity.getPersistentData().m_128459_("z_pos_doma");
         entity.getPersistentData().m_128347_("select", 12.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") == 34.0) {
            if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
               entity.getPersistentData().m_128347_("NameRanged", Math.random());
            }

            for (int index0 = 0; index0 < 22; index0++) {
               num1 = Math.toRadians(Math.random() * 360.0);
               num2 = dis * Mth.m_216263_(RandomSource.m_216327_(), 0.8, 1.0) - 2.0;
               x_pos = x_center + Math.sin(num1) * num2;
               y_pos = y_center + 2.0;
               z_pos = z_center + Math.cos(num1) * num2;

               for (int index1 = 0; index1 < 100; index1++) {
                  if (world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()) {
                     y_pos++;
                  } else if (!world.m_8055_(BlockPos.m_274561_(x_pos, y_pos - 1.0, z_pos)).m_60815_()) {
                     y_pos--;
                  } else if (!world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()
                     && world.m_8055_(BlockPos.m_274561_(x_pos, y_pos - 1.0, z_pos)).m_60815_()) {
                     y_pos++;
                     break;
                  }
               }

               y_pos = --y_pos + Math.random();
               if (world instanceof ServerLevel _serverLevel) {
                  Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_SAND_STONE.get())
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
                              "data merge entity @s {Invulnerable:1b}"
                           );
                     }

                     entityinstance.m_146922_((float)(Math.random() * 360.0));
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
                     if (entityinstance instanceof LivingEntity _livingEntity24
                        && _livingEntity24.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                        _livingEntity24.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(Mth.m_216263_(RandomSource.m_216327_(), 1.0, 2.0));
                     }

                     _serverLevel.m_7967_(entityinstance);
                  }
               }
            }

            for (int index2 = 0; index2 < 8; index2++) {
               distance = dis - Mth.m_216263_(RandomSource.m_216327_(), 4.0, 6.0);
               yaw = Math.toRadians(Mth.m_216263_(RandomSource.m_216327_(), 0.0, 360.0));
               pitch = Math.toRadians(Mth.m_216263_(RandomSource.m_216327_(), -30.0, -5.0));
               x_pos = x_center + Math.cos(yaw) * Math.cos(pitch) * distance;
               y_pos = y_center + Math.sin(pitch) * -1.0 * distance;
               z_pos = z_center + Math.sin(yaw) * Math.cos(pitch) * distance;
               if (world instanceof ServerLevel) {
                  ServerLevel _serverLevelx = (ServerLevel)world;
                  Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_DOMAIN_ISHIGORI.get())
                     .m_262451_(_serverLevelx, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
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

                     entityinstance.getPersistentData().m_128347_("NameRanged_ranged", entity.getPersistentData().m_128459_("NameRanged"));
                     entityinstance.getPersistentData().m_128359_("OWNER_UUID", entity.m_20149_());
                     if (entityinstance instanceof LivingEntity) {
                        LivingEntity _livingEntity39 = (LivingEntity)entityinstance;
                        if (_livingEntity39.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                           _livingEntity39.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get())
                              .m_22100_(Mth.m_216263_(RandomSource.m_216327_(), 3.0, 4.0));
                        }
                     }

                     if (entityinstance instanceof EntityDomainIshigoriEntity) {
                        PlayAnimationEntity2Procedure.execute(entityinstance, "idle" + Math.round((float)Mth.m_216271_(RandomSource.m_216327_(), 1, 4)));
                     }

                     entityinstance.m_7618_(Anchor.EYES, new Vec3(x_center, y_center, z_center));
                     _serverLevelx.m_7967_(entityinstance);
                  }
               }
            }

            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(x_center, y_center, z_center),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.m_216263_(RandomSource.m_216327_(), 0.9, 1.1)
                  );
               } else {
                  _level.m_7785_(
                     x_center,
                     y_center,
                     z_center,
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.blast")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.m_216263_(RandomSource.m_216327_(), 0.9, 1.1),
                     false
                  );
               }
            }
         }

         PlayAnimationProcedure.execute(world, entity);
      }
   }
}
