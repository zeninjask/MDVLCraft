package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.TakadaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainExpansionTodoActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         boolean logic_a = false;
         Entity entity_a = null;
         double z_center = 0.0;
         double range = 0.0;
         double x_pos = 0.0;
         double y_center = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         double x_center = 0.0;
         double rad = 0.0;
         double rad_now = 0.0;
         double pitch = 0.0;
         double y_pos = 0.0;
         range = MapVariables.get(world).DomainExpansionRadius * 2.0;
         x_center = entity.getPersistentData().m_128459_("x_pos_doma");
         y_center = entity.getPersistentData().m_128459_("y_pos_doma");
         z_center = entity.getPersistentData().m_128459_("z_pos_doma");
         entity_a = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("takada_UUID"));
         if (!entity.getPersistentData().m_128471_("Failed")) {
            yaw = Math.toRadians((double)(entity.m_146908_() + 90.0F) + 67.5);
            pitch = Math.toRadians((double)entity.m_146909_());
            x_pos = entity.m_20185_() + Math.cos(yaw) * Math.cos(pitch) * (0.5 + (double)entity.m_20205_());
            y_pos = entity.m_20186_();
            z_pos = entity.m_20189_() + Math.sin(yaw) * Math.cos(pitch) * (0.5 + (double)entity.m_20205_());
            if (!(entity_a instanceof TakadaEntity)) {
               int var10000;
               label210: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                     var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                     break label210;
                  }

                  var10000 = 0;
               }

               if (var10000 % 20 == 10 && world instanceof ServerLevel _serverLevel) {
                  Entity entityinstance = ((EntityType)JujutsucraftModEntities.TAKADA.get())
                     .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                  if (entityinstance != null) {
                     entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                     SetRangedAmmoProcedure.execute(entity, entityinstance);
                     entityinstance.getPersistentData().m_128379_("domain_entity", true);
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

                     entity.getPersistentData().m_128359_("takada_UUID", entityinstance.m_20149_());
                     entity_a = entityinstance;
                     _serverLevel.m_7967_(entityinstance);
                  }
               }
            }
         }

         if (entity_a instanceof TakadaEntity) {
            entity_a.getPersistentData().m_128379_("canFly", true);
            if (!entity_a.getPersistentData().m_128471_("PRESS_Z")) {
               entity_a.getPersistentData().m_128379_("PRESS_Z", entity.getPersistentData().m_128471_("PRESS_Z"));
            } else {
               if (entity.getPersistentData().m_128459_("skill") <= 0.0 || entity.getPersistentData().m_128459_("skill") % 100.0 == 2.0) {
                  entity_a.getPersistentData().m_128379_("PRESS_Z", entity.getPersistentData().m_128471_("PRESS_Z"));
               }

               if (entity_a.getPersistentData().m_128459_("skill") > 0.0 && !entity_a.getPersistentData().m_128471_("PRESS_Z")) {
                  Vec3 _center = new Vec3(x_center, y_center, z_center);

                  for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                     if (entity != entityiterator
                        && entityiterator instanceof LivingEntity
                        && entityiterator.m_6084_()
                        && !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("forge:ranged_ammo")))
                        && LogicAttackDomainProcedure.execute(world, entity, entityiterator)
                        && LogicAttackProcedure.execute(world, entity, entityiterator)) {
                        if (world instanceof Level) {
                           Level _level = (Level)world;
                           if (!_level.m_5776_()) {
                              _level.m_5594_(
                                 null,
                                 BlockPos.m_274561_(entity_a.m_20185_(), entity_a.m_20186_(), entity_a.m_20189_()),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:clap")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _level.m_7785_(
                                 entity_a.m_20185_(),
                                 entity_a.m_20186_(),
                                 entity_a.m_20189_(),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:clap")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }

                        entity_a.m_6021_(
                           entityiterator.m_20185_() - entity_a.m_20154_().f_82479_,
                           entityiterator.m_20186_() - entity_a.m_20154_().f_82480_,
                           entityiterator.m_20189_() - entity_a.m_20154_().f_82481_
                        );
                        if (entity_a instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_
                              .m_9774_(
                                 entityiterator.m_20185_() - entity_a.m_20154_().f_82479_,
                                 entityiterator.m_20186_() - entity_a.m_20154_().f_82480_,
                                 entityiterator.m_20189_() - entity_a.m_20154_().f_82481_,
                                 entity_a.m_146908_(),
                                 entity_a.m_146909_()
                              );
                        }

                        entity_a.m_7618_(
                           Anchor.EYES,
                           new Vec3(entityiterator.m_20185_(), entityiterator.m_20186_() + (double)entityiterator.m_20206_() * 0.5, entityiterator.m_20189_())
                        );
                        if (world instanceof Level) {
                           Level _level = (Level)world;
                           if (!_level.m_5776_()) {
                              _level.m_5594_(
                                 null,
                                 BlockPos.m_274561_(entity_a.m_20185_(), entity_a.m_20186_(), entity_a.m_20189_()),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:clap")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F
                              );
                           } else {
                              _level.m_7785_(
                                 entity_a.m_20185_(),
                                 entity_a.m_20186_(),
                                 entity_a.m_20189_(),
                                 (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:clap")),
                                 SoundSource.NEUTRAL,
                                 1.0F,
                                 1.0F,
                                 false
                              );
                           }
                        }
                     }
                  }

                  if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 15, 0, false, false));
                  }
               }
            }

            entity_a.m_6858_(entity.m_20142_());
            entity_a.m_20260_(entity.m_6144_());
            if (entity_a instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               MobEffectInstance var10001;
               int var10005;
               label182: {
                  var10001 = new MobEffectInstance;
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                     var10005 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                     break label182;
                  }

                  var10005 = 0;
               }

               var10001./* $VF: Unable to resugar constructor */<init>(MobEffects.f_19600_, 60, var10005 * 1, false, false);
               _entity.m_7292_(var10001);
            }

            label221: {
               if (entity instanceof LivingEntity _livEnt68 && _livEnt68.m_21023_((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
                  break label221;
               }

               entity_a.m_6021_(x_pos, y_pos, z_pos);
               if (entity_a instanceof ServerPlayer _serverPlayer) {
                  _serverPlayer.f_8906_.m_9774_(x_pos, y_pos, z_pos, entity_a.m_146908_(), entity_a.m_146909_());
               }

               entity_a.m_146922_(entity.m_146908_());
               entity_a.m_146926_(entity.m_146909_());
               entity_a.m_5618_(entity_a.m_146908_());
               entity_a.m_5616_(entity_a.m_146908_());
               entity_a.f_19859_ = entity_a.m_146908_();
               entity_a.f_19860_ = entity_a.m_146909_();
               if (entity_a instanceof LivingEntity _entity) {
                  _entity.f_20884_ = _entity.m_146908_();
                  _entity.f_20886_ = _entity.m_146908_();
               }
            }

            entity_a.m_20256_(new Vec3(0.0, 0.0, 0.0));
            label153:
            if (entity instanceof LivingEntity _livEnt74 && _livEnt74.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
               if (entity_a instanceof LivingEntity _livEnt75 && _livEnt75.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                  break label153;
               }

               ResetCounterProcedure.execute(entity_a);
               entity_a.getPersistentData().m_128347_("skill", entity.getPersistentData().m_128459_("skill"));
               if (entity_a instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  MobEffectInstance var65;
                  MobEffect var10003;
                  int var10004;
                  label163: {
                     var65 = new MobEffectInstance;
                     var10003 = (MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get();
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                        var10004 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()).m_19557_();
                        break label163;
                     }

                     var10004 = 0;
                  }

                  int var66;
                  label158: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                        var66 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()).m_19564_();
                        break label158;
                     }

                     var66 = 0;
                  }

                  var65./* $VF: Unable to resugar constructor */<init>(var10003, var10004, var66, false, false);
                  _entity.m_7292_(var65);
               }
            }

            if (!entity.m_6084_() && !entity_a.m_9236_().m_5776_() && entity_a.m_20194_() != null) {
               entity_a.m_20194_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                        CommandSource.f_80164_,
                        entity_a.m_20182_(),
                        entity_a.m_20155_(),
                        entity_a.m_9236_() instanceof ServerLevel ? (ServerLevel)entity_a.m_9236_() : null,
                        4,
                        entity_a.m_7755_().getString(),
                        entity_a.m_5446_(),
                        entity_a.m_9236_().m_7654_(),
                        entity_a
                     ),
                     "kill @s"
                  );
            }
         }

         if (!entity.getPersistentData().m_128471_("Failed")) {
            dis = range * 0.5 - 4.0;
            yaw = Math.toRadians(Math.random() * 360.0);
            rad_now = Math.toRadians((Math.random() - 0.5) * 180.0);
            logic_a = Math.random() < 0.5;

            for (int index0 = 0; index0 < 72; index0++) {
               if (Math.random() < 0.5) {
                  if (logic_a) {
                     x_pos = x_center + Math.cos(rad) * (Math.cos(rad_now) + Math.abs(Math.sin(yaw) * Math.sin(rad_now))) * dis;
                     y_pos = y_center + Math.sin(rad) * Math.sin(rad_now) * -1.0 * dis;
                     z_pos = z_center + Math.sin(rad) * (Math.cos(rad_now) + Math.abs(Math.cos(yaw) * Math.sin(rad_now))) * dis;
                  } else {
                     x_pos = x_center + Math.cos(rad) * Math.cos(rad_now) * dis;
                     y_pos = y_center + Math.sin(rad) * dis;
                     z_pos = z_center + Math.cos(rad) * Math.sin(rad_now) * dis;
                  }

                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123748_, x_pos, y_pos, z_pos, 1, 0.5, 0.5, 0.5, 0.0);
                  }
               }

               rad += Math.toRadians(10.0 * Math.random());
            }
         }
      }
   }
}
