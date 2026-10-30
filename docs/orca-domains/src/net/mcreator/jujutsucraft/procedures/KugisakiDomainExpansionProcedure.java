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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class KugisakiDomainExpansionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double pitch_s = 0.0;
         double yaw_s = 0.0;
         double num1 = 0.0;
         double x_pos = 0.0;
         double pitch = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         entity.getPersistentData().m_128347_("select", 34.0);
         DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().m_128459_("cnt1") > 0.0) {
            if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
               if (entity instanceof LivingEntity _entity) {
                  _entity.m_21011_(InteractionHand.MAIN_HAND, true);
               }

               if (world instanceof Level _level) {
                  if (!_level.m_5776_()) {
                     _level.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_gateway.spawn")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.8F
                     );
                  } else {
                     _level.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_gateway.spawn")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.8F,
                        false
                     );
                  }
               }
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.m_8767_(ParticleTypes.f_123765_, x, y, z, 40, 3.0, 0.0, 3.0, 0.05);
            }

            if (entity.getPersistentData().m_128459_("cnt1") > 33.0 && entity.getPersistentData().m_128459_("cnt1") == 34.0) {
               dis = MapVariables.get(world).DomainExpansionRadius;
               if (world instanceof Level _levelx) {
                  if (!_levelx.m_5776_()) {
                     _levelx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_portal.spawn")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.8F
                     );
                  } else {
                     _levelx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_portal.spawn")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.8F,
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxx) {
                  _levelxx.m_8767_(ParticleTypes.f_123765_, x, y, z, 40, 3.0, 0.0, 3.0, 0.25);
               }

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

               num1 = 0.0;
               yaw = Math.toRadians((double)(entity.m_146908_() + 90.0F));
               pitch = Math.toRadians((double)entity.m_146909_());
               if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
                  entity.getPersistentData().m_128347_("NameRanged", Math.random());
               }

               for (int index0 = 0; index0 < 8; index0++) {
                  x_pos = entity.getPersistentData().m_128459_("x_pos_doma") - Math.cos(yaw) * Math.cos(pitch) * (dis - 8.0);
                  y_pos = entity.getPersistentData().m_128459_("y_pos_doma");
                  z_pos = entity.getPersistentData().m_128459_("z_pos_doma") - Math.sin(yaw) * Math.cos(pitch) * (dis - 8.0);
                  if (num1 % 2.0 == 0.0) {
                     if (world instanceof ServerLevel _serverLevel) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_DOMAIN_KUGISAKI.get())
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

                           entityinstance.m_146922_((float)(Math.toDegrees(yaw) - 90.0));
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
                  } else if (world instanceof ServerLevel _serverLevelx) {
                     Entity entityinstance = ((EntityType)JujutsucraftModEntities.ENTITY_TREE.get())
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

                        entityinstance.m_146922_((float)(Math.toDegrees(yaw) - 90.0));
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
                        if (entityinstance instanceof LivingEntity _livingEntity46
                           && _livingEntity46.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                           _livingEntity46.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_(2.5);
                        }

                        _serverLevelx.m_7967_(entityinstance);
                     }
                  }

                  yaw += Math.toRadians(45.0);
                  num1++;
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
         }

         PlayAnimationProcedure.execute(world, entity);
      }
   }
}
