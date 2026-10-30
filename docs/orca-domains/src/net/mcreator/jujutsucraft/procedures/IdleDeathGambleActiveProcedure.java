package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.SlotEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class IdleDeathGambleActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double num1;
         double num2;
         double range;
         label285: {
            double z_center = 0.0;
            range = 0.0;
            double x_pos = 0.0;
            double tick = 0.0;
            double y_center = 0.0;
            double z_pos = 0.0;
            double yaw = 0.0;
            double num_reserve = 0.0;
            double x_center = 0.0;
            num1 = 0.0;
            double pitch = 0.0;
            double y_pos = 0.0;
            double num3 = 0.0;
            num2 = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               double var77 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label285;
            }

            double var10000 = 0.0;
         }

         if (!entity.getPersistentData().m_128471_("Failed")) {
            if (entity.getPersistentData().m_128459_("Reserve") != 0.0 && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 2, 0, false, false));
            }

            if (entity.getPersistentData().m_128459_("skill") == 0.0) {
               if (!(entity.getPersistentData().m_128459_("Reserve") > 0.0)) {
                  num1 = 0.0;

                  for (int index1 = 0; index1 < 3; index1++) {
                     if (!(GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("SLOT" + Math.round(++num1))) instanceof LivingEntity)
                        )
                      {
                        num2 = 0.0;
                        entity.getPersistentData().m_128347_("Reserve", 0.0);

                        for (int index2 = 0; index2 < 3; index2++) {
                           num2++;
                           entity.getPersistentData().m_128359_("SLOT" + Math.round(num1), "");
                        }
                        break;
                     }
                  }
               } else {
                  if (entity.getPersistentData().m_128459_("NameRanged") == 0.0) {
                     entity.getPersistentData().m_128347_("NameRanged", Math.random());
                  }

                  double var42 = (double)entity.m_146908_();
                  double var47 = (double)entity.m_146909_();
                  double var44 = entity.getPersistentData().m_128459_("x_pos_doma");
                  double var39 = entity.getPersistentData().m_128459_("y_pos_doma");
                  double var35 = entity.getPersistentData().m_128459_("z_pos_doma");
                  RotateEntityProcedure.execute(var44, var39, var35, entity);
                  double var48 = var39 + 2.0;
                  double var43 = entity.getPersistentData().m_128459_("Reserve");
                  if (var43 < 5.0 && Math.random() < var43 * 0.05) {
                     var43 = 13.0;
                  }

                  for (int index0 = 0; index0 < 3; index0++) {
                     if (++num1 == 1.0) {
                        num2 = 0.0;
                     } else if (num1 == 2.0) {
                        num2 = 90.0;
                     } else if (num1 == 3.0) {
                        num2 = -90.0;
                     }

                     double var37 = var44
                        + Math.cos(Math.toRadians((double)(entity.m_146908_() + 90.0F))) * (range * 0.5 - 8.0)
                        + Math.cos(Math.toRadians((double)(entity.m_146908_() + 90.0F) + num2)) * Math.min(4.5, Math.abs(num2));
                     double var40 = var35
                        + Math.sin(Math.toRadians((double)(entity.m_146908_() + 90.0F))) * (range * 0.5 - 8.0)
                        + Math.sin(Math.toRadians((double)(entity.m_146908_() + 90.0F) + num2)) * Math.min(4.5, Math.abs(num2));
                     if (world instanceof Level _level) {
                        if (!_level.m_5776_()) {
                           _level.m_5594_(
                              null,
                              BlockPos.m_274561_(var37, var48, var40),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")),
                              SoundSource.NEUTRAL,
                              5.0F,
                              1.0F
                           );
                        } else {
                           _level.m_7785_(
                              var37,
                              var48,
                              var40,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:gacon")),
                              SoundSource.NEUTRAL,
                              5.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     if (world instanceof ServerLevel _serverLevel) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.SLOT.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var37, var48, var40), MobSpawnType.MOB_SUMMONED, false, false);
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

                           entityinstance.m_146922_(entity.m_146908_() + 180.0F);
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
                           if (entityinstance instanceof SlotEntity _datEntSetI) {
                              _datEntSetI.m_20088_().m_135381_(SlotEntity.DATA_SLOT_MODE, (int)(4.0 - num1));
                           }

                           entity.getPersistentData().m_128359_("SLOT" + Math.round(4.0 - num1), entityinstance.m_20149_());
                           double var50 = 0.0;
                           if (!(var43 < 10.0)) {
                              if (entityinstance instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                 _entity.m_7292_(new MobEffectInstance(MobEffects.f_19590_, Integer.MAX_VALUE, 10, false, false));
                              }
                           } else {
                              if (var43 > 0.0 && var43 < 5.0) {
                                 var50 = (var43 - 1.0) * 2.0;
                                 if (entity.getPersistentData().m_128459_("mode_hakari") == 1.0 || entity.getPersistentData().m_128459_("mode_hakari") == 2.0) {
                                    var50 += 4.0;
                                 }

                                 var50 = Math.min(var50, 9.0);
                              } else if (var43 >= 5.0) {
                                 var50 = 10.0;
                              }

                              if (var50 > 0.0 && entityinstance instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                 _entity.m_7292_(new MobEffectInstance(MobEffects.f_19621_, Integer.MAX_VALUE, (int)(var50 - 1.0), false, false));
                              }
                           }

                           if ((entity.getPersistentData().m_128459_("mode_hakari") == 1.0 || entity.getPersistentData().m_128459_("mode_hakari") >= 10.0)
                              && entityinstance instanceof LivingEntity _entity
                              && !_entity.m_9236_().m_5776_()) {
                              _entity.m_7292_(new MobEffectInstance(MobEffects.f_19596_, Integer.MAX_VALUE, 4, false, false));
                           }

                           _serverLevel.m_7967_(entityinstance);
                        }
                     }
                  }

                  entity.m_146922_((float)var42);
                  entity.m_146926_((float)var47);
                  entity.m_5618_(entity.m_146908_());
                  entity.m_5616_(entity.m_146908_());
                  entity.f_19859_ = entity.m_146908_();
                  entity.f_19860_ = entity.m_146909_();
                  if (entity instanceof LivingEntity _entity) {
                     _entity.f_20884_ = _entity.m_146908_();
                     _entity.f_20886_ = _entity.m_146908_();
                  }

                  entity.getPersistentData().m_128347_("Reserve", entity.getPersistentData().m_128459_("Reserve") * -1.0);
                  if (entity instanceof LivingEntity _livEnt49
                     && _livEnt49.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                     && entity instanceof LivingEntity _entity
                     && !_entity.m_9236_().m_5776_()) {
                     MobEffectInstance var10001;
                     MobEffect var10003;
                     int var10005;
                     label269: {
                        var10001 = new MobEffectInstance;
                        var10003 = (MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get();
                        if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                           var10005 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                           break label269;
                        }

                        var10005 = 0;
                     }

                     var10001./* $VF: Unable to resugar constructor */<init>(var10003, 100, var10005, false, false);
                     _entity.m_7292_(var10001);
                  }
               }
            }

            if (entity.getPersistentData().m_128459_("mode_hakari") == 2.0) {
               if (Math.random() < 0.025) {
                  num1 = Math.toRadians(Math.random() * 360.0);
                  num2 = range / 2.0 - 4.0;
                  double var38 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(num1) * num2;
                  double var49 = entity.getPersistentData().m_128459_("y_pos_doma") + 5.0;
                  double var41 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(num1) * num2;
                  if (Math.random() < 0.25) {
                     if (world instanceof ServerLevel _levelx) {
                        _levelx.m_7654_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                    CommandSource.f_80164_,
                                    new Vec3(var38, var49, var41),
                                    Vec2.f_82462_,
                                    _levelx,
                                    4,
                                    "",
                                    Component.m_237113_(""),
                                    _levelx.m_7654_(),
                                    null
                                 )
                                 .m_81324_(),
                              "summon firework_rocket ~ ~ ~ {LifeTime:10,FireworksItem:{id:\"firework_rocket\",Count:1,tag:{Fireworks:{Explosions:[{Type:"
                                 + Math.round(Math.random() * 4.0)
                                 + ",Flicker:0b,Trail:0b,Colors:[I;16711680],FadeColors:[I;16711680]}]}}}}"
                           );
                     }
                  } else if (Math.random() < 0.25) {
                     if (world instanceof ServerLevel _levelx) {
                        _levelx.m_7654_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                    CommandSource.f_80164_,
                                    new Vec3(var38, var49, var41),
                                    Vec2.f_82462_,
                                    _levelx,
                                    4,
                                    "",
                                    Component.m_237113_(""),
                                    _levelx.m_7654_(),
                                    null
                                 )
                                 .m_81324_(),
                              "summon firework_rocket ~ ~ ~ {LifeTime:10,FireworksItem:{id:\"firework_rocket\",Count:1,tag:{Fireworks:{Explosions:[{Type:"
                                 + Math.round(Math.random() * 4.0)
                                 + ",Flicker:0b,Trail:0b,Colors:[I;16774912],FadeColors:[I;16774912]}]}}}}"
                           );
                     }
                  } else if (Math.random() < 0.25) {
                     if (world instanceof ServerLevel _levelx) {
                        _levelx.m_7654_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                    CommandSource.f_80164_,
                                    new Vec3(var38, var49, var41),
                                    Vec2.f_82462_,
                                    _levelx,
                                    4,
                                    "",
                                    Component.m_237113_(""),
                                    _levelx.m_7654_(),
                                    null
                                 )
                                 .m_81324_(),
                              "summon firework_rocket ~ ~ ~ {LifeTime:10,FireworksItem:{id:\"firework_rocket\",Count:1,tag:{Fireworks:{Explosions:[{Type:"
                                 + Math.round(Math.random() * 4.0)
                                 + ",Flicker:0b,Trail:0b,Colors:[I;43775],FadeColors:[I;43775]}]}}}}"
                           );
                     }
                  } else if (world instanceof ServerLevel _levelx) {
                     _levelx.m_7654_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 new Vec3(var38, var49, var41),
                                 Vec2.f_82462_,
                                 _levelx,
                                 4,
                                 "",
                                 Component.m_237113_(""),
                                 _levelx.m_7654_(),
                                 null
                              )
                              .m_81324_(),
                           "summon firework_rocket ~ ~ ~ {LifeTime:10,FireworksItem:{id:\"firework_rocket\",Count:1,tag:{Fireworks:{Explosions:[{Type:"
                              + Math.round(Math.random() * 4.0)
                              + ",Flicker:0b,Trail:0b,Colors:[I;65442],FadeColors:[I;65442]}]}}}}"
                        );
                  }
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.m_8767_(
                     ParticleTypes.f_123767_,
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma"),
                     10,
                     range * 0.25,
                     range * 0.25,
                     range * 0.25,
                     0.1
                  );
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.m_8767_(
                     ParticleTypes.f_175830_,
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma"),
                     10,
                     range * 0.25,
                     range * 0.25,
                     range * 0.25,
                     0.1
                  );
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.m_8767_(
                     ParticleTypes.f_175827_,
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma"),
                     10,
                     range * 0.25,
                     range * 0.25,
                     range * 0.25,
                     0.1
                  );
               }
            }

            if (entity.getPersistentData().m_128471_("StartDomainAttack")) {
               entity.getPersistentData().m_128379_("StartDomainAttack", false);
               Vec3 _center = new Vec3(
                  entity.getPersistentData().m_128459_("x_pos_doma"),
                  entity.getPersistentData().m_128459_("y_pos_doma"),
                  entity.getPersistentData().m_128459_("z_pos_doma")
               );

               for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                  if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
                     entityiterator.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entityiterator.m_20182_(),
                              entityiterator.m_20155_(),
                              entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                              4,
                              entityiterator.m_7755_().getString(),
                              entityiterator.m_5446_(),
                              entityiterator.m_9236_().m_7654_(),
                              entityiterator
                           ),
                           "title @s title {\"text\":\"" + Component.m_237115_("jujutsu.technique.hakari1").getString() + "\"}"
                        );
                  }

                  if (entity.getPersistentData().m_128459_("mode_hakari") == 0.0) {
                     if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
                        entityiterator.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 entityiterator.m_20182_(),
                                 entityiterator.m_20155_(),
                                 entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                                 4,
                                 entityiterator.m_7755_().getString(),
                                 entityiterator.m_5446_(),
                                 entityiterator.m_9236_().m_7654_(),
                                 entityiterator
                              ),
                              "title @s subtitle {\"text\":\"" + Component.m_237115_("jujutsu.technique.hakari2").getString() + "\"}"
                           );
                     }
                  } else if (entity.getPersistentData().m_128459_("mode_hakari") == 2.0) {
                     if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
                        entityiterator.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 entityiterator.m_20182_(),
                                 entityiterator.m_20155_(),
                                 entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                                 4,
                                 entityiterator.m_7755_().getString(),
                                 entityiterator.m_5446_(),
                                 entityiterator.m_9236_().m_7654_(),
                                 entityiterator
                              ),
                              "title @s subtitle {\"text\":\"" + Component.m_237115_("jujutsu.technique.hakari3").getString() + "\"}"
                           );
                     }
                  } else if (!entityiterator.m_9236_().m_5776_() && entityiterator.m_20194_() != null) {
                     entityiterator.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entityiterator.m_20182_(),
                              entityiterator.m_20155_(),
                              entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                              4,
                              entityiterator.m_7755_().getString(),
                              entityiterator.m_5446_(),
                              entityiterator.m_9236_().m_7654_(),
                              entityiterator
                           ),
                           "title @s subtitle {\"text\":\"" + Component.m_237115_("jujutsu.technique.hakari4").getString() + "\"}"
                        );
                  }
               }
            }
         }
      }
   }
}
