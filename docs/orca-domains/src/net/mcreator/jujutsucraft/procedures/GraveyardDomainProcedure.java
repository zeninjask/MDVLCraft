package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class GraveyardDomainProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double x_center = 0.0;
         double z_center = 0.0;
         double num1 = 0.0;
         double x_pos = 0.0;
         double y_center = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double num3 = 0.0;
         double dis = 0.0;
         double num2 = 0.0;
         dis = MapVariables.get(world).DomainExpansionRadius;
         x_center = entity.getPersistentData().m_128459_("x_pos_doma");
         y_center = entity.getPersistentData().m_128459_("y_pos_doma");
         z_center = entity.getPersistentData().m_128459_("z_pos_doma");
         entity.getPersistentData().m_128347_("select", 25.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") > 0.0) {
            if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
               if (world instanceof Level _level) {
                  if (!_level.m_5776_()) {
                     _level.m_5594_(
                        null,
                        BlockPos.m_274561_(x_center, y_center, z_center),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:slow_motion_end")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        0.5F
                     );
                  } else {
                     _level.m_7785_(
                        x_center,
                        y_center,
                        z_center,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:slow_motion_end")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        0.5F,
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.m_5776_()) {
                     _levelx.m_5594_(
                        null,
                        BlockPos.m_274561_(x_center, y_center, z_center),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:piano_horror")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        0.5F
                     );
                  } else {
                     _levelx.m_7785_(
                        x_center,
                        y_center,
                        z_center,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:piano_horror")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        0.5F,
                        false
                     );
                  }
               }
            }

            if (entity.getPersistentData().m_128459_("cnt1") == 34.0) {
               if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
                  entity.getPersistentData().m_128347_("NameRanged", Math.random());
               }

               for (int index0 = 0; index0 < 24; index0++) {
                  num3++;
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

                  for (int index2 = 0; index2 < 16; index2++) {
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

                  if (num3 < 16.0) {
                     if (Math.random() < 0.5) {
                        if (world instanceof ServerLevel _serverLevel) {
                           Entity entityinstance = ((EntityType)JujutsucraftModEntities.GRAVESTONE_1.get())
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
                     } else if (world instanceof ServerLevel _serverLevelx) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.GRAVESTONE_2.get())
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
                           _serverLevelx.m_7967_(entityinstance);
                        }
                     }
                  } else if (world instanceof ServerLevel _serverLevelxx) {
                     Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_TREE.get())
                        .m_262451_(_serverLevelxx, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
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
                        if (entityinstance instanceof LivingEntity _livingEntity48
                           && _livingEntity48.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                           _livingEntity48.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(2.0);
                        }

                        _serverLevelxx.m_7967_(entityinstance);
                     }
                  }
               }
            }

            num1 = Math.toRadians(Math.random() * 360.0);
            num2 = entity.getPersistentData().m_128459_("cnt1") * 0.5;
            num3 = Math.toRadians(4.0);

            for (int index3 = 0; index3 < 90; index3++) {
               x_pos = x_center + Math.sin(num1) * num2;
               z_pos = z_center + Math.cos(num1) * num2;
               if (Math.random() < 0.5 && world instanceof ServerLevel _levelxx) {
                  _levelxx.m_8767_(ParticleTypes.f_123765_, x_pos, y_center, z_pos, 1, 0.25, Math.max(num2 / 10.0, 0.25), 0.25, 0.0);
               }

               if (Math.random() < 0.5 && world instanceof ServerLevel _levelxx) {
                  _levelxx.m_8767_(ParticleTypes.f_123746_, x_pos, y_center, z_pos, 1, 0.25, Math.max(num2 / 10.0, 0.25), 0.25, 0.0);
               }

               num1 += num3;
            }
         }

         PlayAnimationProcedure.execute(world, entity);
      }
   }
}
