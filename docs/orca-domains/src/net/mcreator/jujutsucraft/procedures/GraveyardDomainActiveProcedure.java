package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.CoffinEntity;
import net.mcreator.jujutsucraft.entity.CursedSpiritGrade05Entity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class GraveyardDomainActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         boolean failed;
         Entity entity_b;
         double range;
         double var10000;
         label269: {
            boolean GRAVESTONE = false;
            failed = false;
            Entity entity_a = null;
            entity_b = null;
            double old_skill = 0.0;
            double HP = 0.0;
            range = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double tick = 0.0;
            double z_pos = 0.0;
            double y_pos = 0.0;
            double MAX_ST = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label269;
            }

            var10000 = 0.0;
         }

         double var39 = var10000;
         double var33 = entity.getPersistentData().m_128459_("skill");
         double var37 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
         double var38 = entity.getPersistentData().m_128459_("x_pos_doma");
         double var41 = entity.getPersistentData().m_128459_("y_pos_doma");
         double var40 = entity.getPersistentData().m_128459_("z_pos_doma");
         if (world instanceof ServerLevel _level) {
            _level.m_8767_(ParticleTypes.f_123783_, var38, var41 + 0.0, var40, 20, range * 0.175, 1.0, range * 0.175, 0.05);
         }

         if (world instanceof ServerLevel _level) {
            _level.m_8767_(ParticleTypes.f_123746_, var38, var41 + 0.0, var40, 1, range * 0.175, 0.1, range * 0.175, 0.05);
         }

         if (!entity.getPersistentData().m_128471_("Failed")) {
            entity.getPersistentData().m_128347_("skill", 2505.0);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 50.0);
            if (var39 % 100.0 == 1.0) {
               entity.getPersistentData().m_128379_("StartDomainAttack", true);
            }

            label259:
            if (entity.getPersistentData().m_128471_("StartDomainAttack")) {
               if (entity instanceof LivingEntity _livEnt13 && _livEnt13.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                  break label259;
               }

               boolean var30 = false;
               Entity var31 = null;
               if (entity.getPersistentData().m_128459_("NameRanged") != 0.0) {
                  Vec3 _center = new Vec3(
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma")
                  );

                  for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                     if (entityiterator instanceof CoffinEntity
                        && entityiterator.m_6084_()
                        && entity != entityiterator
                        && entity.getPersistentData().m_128459_("NameRanged") == entityiterator.getPersistentData().m_128459_("NameRanged_ranged")
                        && entityiterator.getPersistentData().m_128459_("death_count1") == 0.0
                        && entityiterator.getPersistentData().m_128459_("death_count2") == 0.0) {
                        var30 = true;
                        var38 = entityiterator.m_20185_();
                        var41 = entityiterator.m_20186_();
                        var40 = entityiterator.m_20189_();
                        var31 = entityiterator;
                        entity_b = GetEntityFromUUIDProcedure.execute(world, entityiterator.getPersistentData().m_128461_("TARGET"));
                        if (entity_b instanceof LivingEntity
                           && entity_b instanceof LivingEntity _livEnt31
                           && _livEnt31.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                           failed = true;
                        }
                        break;
                     }
                  }
               }

               if (var30) {
                  if (entity instanceof CursedSpiritGrade05Entity) {
                     PlayAnimationEntity2Procedure.execute(entity, "right_arm_up");
                  }

                  if (var31 instanceof LivingEntity && !failed) {
                     int var74;
                     label211: {
                        if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                           var74 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                           break label211;
                        }

                        var74 = 0;
                     }

                     double var35 = 75.0 + (double)var74 * 7.5;
                     if (world instanceof ServerLevel _serverLevel) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.GRAVESTONE_3.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var38, var41, var40), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           SetRangedAmmoProcedure.execute(entity, entityinstance);
                           if (entityinstance instanceof LivingEntity _livingEntity38 && _livingEntity38.m_21204_().m_22171_(Attributes.f_22276_)) {
                              _livingEntity38.m_21051_(Attributes.f_22276_).m_22100_(var35);
                           }

                           if (entityinstance instanceof LivingEntity _entity) {
                              _entity.m_21153_((float)var35);
                           }

                           entityinstance.getPersistentData().m_128347_("cnt2", 20.0);
                           entityinstance.m_20256_(new Vec3(0.0, -1.0, 0.0));
                           var31.getPersistentData().m_128359_("CONNECT_UUID", entityinstance.m_20149_());
                           _serverLevel.m_7967_(entityinstance);
                        }
                     }

                     for (int index0 = 0; index0 < 8; index0++) {
                        if (world.m_8055_(BlockPos.m_274561_(var31.m_20185_(), var31.m_20186_(), var31.m_20189_())).m_60815_()) {
                           var31.m_6021_(var31.m_20185_(), var31.m_20186_() - (double)var31.m_20206_(), var31.m_20189_());
                           if (var31 instanceof ServerPlayer _serverPlayer) {
                              _serverPlayer.f_8906_
                                 .m_9774_(var31.m_20185_(), var31.m_20186_() - (double)var31.m_20206_(), var31.m_20189_(), var31.m_146908_(), var31.m_146909_());
                           }

                           if (entity_b instanceof LivingEntity) {
                              entity_b.m_6021_(var31.m_20185_(), var31.m_20186_() + 0.25, var31.m_20189_());
                              if (entity_b instanceof ServerPlayer _serverPlayer) {
                                 _serverPlayer.f_8906_
                                    .m_9774_(var31.m_20185_(), var31.m_20186_() + 0.25, var31.m_20189_(), entity_b.m_146908_(), entity_b.m_146909_());
                              }
                           }
                           break;
                        }

                        var31.m_6021_(var31.m_20185_(), var31.m_20186_() - 1.0, var31.m_20189_());
                        if (var31 instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_.m_9774_(var31.m_20185_(), var31.m_20186_() - 1.0, var31.m_20189_(), var31.m_146908_(), var31.m_146909_());
                        }
                     }
                  }

                  if (!(entity instanceof Player) && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, 6, false, false));
                  }
               } else {
                  var31 = null;
                  double var42 = 0.0;
                  Vec3 _center = new Vec3(
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma")
                  );

                  for (Entity entityiteratorx : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                     if (entity != entityiteratorx
                        && entityiteratorx instanceof LivingEntity
                        && entityiteratorx.m_6084_()
                        && !entityiteratorx.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                        && !entityiteratorx.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:not_living")))
                        && LogicAttackDomainProcedure.execute(world, entity, entityiteratorx)
                        && LogicAttackProcedure.execute(world, entity, entityiteratorx)) {
                        label190: {
                           if (entityiteratorx instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                              var72 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                              break label190;
                           }

                           var72 = 0;
                        }

                        if ((double)var72 >= var42) {
                           var31 = entityiteratorx;
                           var38 = entityiteratorx.m_20185_();
                           var41 = entityiteratorx.m_20186_();
                           var40 = entityiteratorx.m_20189_();
                        }

                        label183: {
                           if (entityiteratorx instanceof LivingEntity) {
                              LivingEntity _livEnt = (LivingEntity)entityiteratorx;
                              if (_livEnt.m_21023_(MobEffects.f_19600_)) {
                                 var10000 = (double)_livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                                 break label183;
                              }
                           }

                           var10000 = 0.0;
                        }

                        var42 = var10000;
                     }
                  }

                  if (var31 instanceof LivingEntity) {
                     if (var31.getPersistentData().m_128471_("attack")) {
                        var31.getPersistentData().m_128347_("skill", 0.0);
                     }

                     if (!var31.m_9236_().m_5776_() && var31.m_20194_() != null) {
                        var31.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 var31.m_20182_(),
                                 var31.m_20155_(),
                                 var31.m_9236_() instanceof ServerLevel ? (ServerLevel)var31.m_9236_() : null,
                                 4,
                                 var31.m_7755_().getString(),
                                 var31.m_5446_(),
                                 var31.m_9236_().m_7654_(),
                                 var31
                              ),
                              "title @s title {\"text\":\"\\uE110\"}"
                           );
                     }

                     int var10001;
                     label228: {
                        if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                           var10001 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                           break label228;
                        }

                        var10001 = 0;
                     }

                     double var34 = (double)(40 + var10001 * 4);
                     if (world instanceof ServerLevel _serverLevelx) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.COFFIN.get())
                           .m_262451_(_serverLevelx, null, null, BlockPos.m_274561_(var38, var41, var40), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           SetRangedAmmoProcedure.execute(entity, entityinstance);
                           entityinstance.m_146922_(var31.m_146908_());
                           entityinstance.m_146926_(0.0F);
                           entityinstance.m_5618_(entityinstance.m_146908_());
                           entityinstance.m_5616_(entityinstance.m_146908_());
                           entityinstance.f_19859_ = entityinstance.m_146908_();
                           entityinstance.f_19860_ = entityinstance.m_146909_();
                           if (entityinstance instanceof LivingEntity _entity) {
                              _entity.f_20884_ = _entity.m_146908_();
                              _entity.f_20886_ = _entity.m_146908_();
                           }

                           if (entityinstance instanceof LivingEntity _livingEntity93 && _livingEntity93.m_21204_().m_22171_(Attributes.f_22276_)) {
                              _livingEntity93.m_21051_(Attributes.f_22276_).m_22100_(var34);
                           }

                           if (entityinstance instanceof LivingEntity _entity) {
                              _entity.m_21153_((float)var34);
                           }

                           entityinstance.getPersistentData().m_128359_("TARGET", var31.m_20149_());
                           _serverLevelx.m_7967_(entityinstance);
                        }
                     }

                     for (int index1 = 0; index1 < 2; index1++) {
                        if (world instanceof Level _level) {
                           if (!_level.m_5776_()) {
                              _level.m_5594_(
                                 null,
                                 BlockPos.m_274561_(var38, var41, var40),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.wooden_door.close")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.0F
                              );
                           } else {
                              _level.m_7785_(
                                 var38,
                                 var41,
                                 var40,
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.wooden_door.close")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }

                        if (world instanceof Level _levelx) {
                           if (!_levelx.m_5776_()) {
                              _levelx.m_5594_(
                                 null,
                                 BlockPos.m_274561_(var38, var41, var40),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _levelx.m_7785_(
                                 var38,
                                 var41,
                                 var40,
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.iron_door.close")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }
                     }

                     if (!(entity instanceof Player) && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 6, false, false));
                     }
                  }
               }

               entity.getPersistentData().m_128379_("StartDomainAttack", false);
            }
         }

         entity.getPersistentData().m_128347_("skill", var33);
         entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var37);
      }
   }
}
