package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.FushiguroMegumiEntity;
import net.mcreator.jujutsucraft.entity.FushiguroMegumiShibuyaEntity;
import net.mcreator.jujutsucraft.entity.KenjakuEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainExpansionCreateBarrierProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         String STR1 = "";
         Entity target_entity = null;
         double x_dis = 0.0;
         double distance = 0.0;
         double z_dis = 0.0;
         double x_pos = 0.0;
         double domainRadius = 0.0;
         double z_pos = 0.0;
         double yaw = 0.0;
         double dis = 0.0;
         double loop_num = 0.0;
         double power = 0.0;
         double pitch = 0.0;
         double y_pos = 0.0;
         double y_dis = 0.0;
         boolean old_failed = false;
         boolean reStart = false;
         boolean update_pos = false;
         Entity var49 = entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null;
         if (entity.getPersistentData().m_128459_("cnt3") < 20.0) {
            entity.getPersistentData().m_128347_("select", 0.0);
            if (entity.getPersistentData().m_128459_("cnt3") == 0.0) {
               ChangeCurseEnergyProcedure.execute(entity, entity.getPersistentData().m_128459_("used_technique_cost"));
            }

            CompoundTag var129;
            int var133;
            double var10002;
            label273: {
               label458: {
                  var129 = entity.getPersistentData();
                  var10002 = entity.getPersistentData().m_128459_("cnt3");
                  if (entity instanceof LivingEntity _livEnt6 && _livEnt6.m_21023_((MobEffect)JujutsucraftModMobEffects.ZONE.get())) {
                     break label458;
                  }

                  label267: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
                        var133 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get()).m_19564_();
                        break label267;
                     }

                     var133 = 0;
                  }

                  if (var133 <= 0) {
                     var133 = 1;
                     break label273;
                  }
               }

               var133 = 2;
            }

            var129.m_128347_("cnt3", var10002 + (double)var133);
            if (entity instanceof Player) {
               if (entity.getPersistentData().m_128471_("PRESS_Z")) {
                  power = (double)Math.round((20.0 - entity.getPersistentData().m_128459_("cnt3")) * 0.25);
                  STR1 = Component.m_237115_("jujutsu.message.long_press").getString();

                  for (int index0 = 0; index0 < (int)power; index0++) {
                     STR1 = "■" + STR1 + "■";
                  }

                  if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                     _player.m_5661_(Component.m_237113_(STR1), true);
                  }

                  if (entity.getPersistentData().m_128459_("cnt3") >= 20.0 && entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                     _player.m_5661_(Component.m_237113_(""), true);
                  }
               } else {
                  if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
                     _player.m_5661_(Component.m_237113_(""), true);
                  }

                  entity.getPersistentData().m_128347_("select", 0.0);
                  entity.getPersistentData().m_128347_("skill", 0.0);
               }
            } else if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 5, 9, false, false));
            }
         } else {
            domainRadius = MapVariables.get(world).DomainExpansionRadius;
            entity.getPersistentData().m_128347_("cnt1", entity.getPersistentData().m_128459_("cnt1") + 1.0);
            entity.m_20256_(new Vec3(0.0, Math.min(entity.m_20184_().m_7098_(), 0.0), 0.0));
            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 5, 9, false, false));
            }

            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.ATTACKING.get(), 5, 9, false, false));
            }

            if (entity.getPersistentData().m_128459_("cnt7") == 0.0) {
               entity.getPersistentData().m_128347_("cnt7", 1.0);
               ChangeCurseEnergyProcedure.execute(entity, entity.getPersistentData().m_128459_("used_technique_cost") * -1.0);
               if (entity instanceof Player && Math.random() < 0.01 && entity instanceof ServerPlayer _player) {
                  Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:mastery_open_barrier_type_domain"));
                  AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                  if (!_ap.m_8193_()) {
                     for (String criteria : _ap.m_8219_()) {
                        _player.m_8960_().m_135988_(_adv, criteria);
                     }
                  }
               }

               if (var49 instanceof LivingEntity) {
                  RotateEntityProcedure.execute(var49.m_20185_(), var49.m_20186_() + (double)var49.m_20206_() * 0.5, var49.m_20189_(), entity);
               }

               label462: {
                  if (entity instanceof LivingEntity _livEnt36 && _livEnt36.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                     break label462;
                  }

                  yaw = (double)entity.m_146908_();
                  pitch = (double)entity.m_146909_();
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

                  entity.getPersistentData()
                     .m_128347_(
                        "x_pos_doma",
                        (double)Math.round(
                           entity.m_20185_()
                              + Math.cos(Math.toRadians((double)(entity.m_146908_() + 90.0F))) * Math.cos(Math.toRadians(0.0)) * domainRadius * 0.5
                        )
                     );
                  entity.getPersistentData().m_128347_("y_pos_doma", (double)Math.round(entity.m_20186_()));
                  entity.getPersistentData()
                     .m_128347_(
                        "z_pos_doma",
                        (double)Math.round(
                           entity.m_20189_()
                              + Math.sin(Math.toRadians((double)(entity.m_146908_() + 90.0F))) * Math.cos(Math.toRadians(0.0)) * domainRadius * 0.5
                        )
                     );
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

               if ((entity.getPersistentData().m_128459_("select") == 1.0 || entity.getPersistentData().m_128459_("select") == 18.0)
                  && (
                     !(entity instanceof Player)
                        ? entity instanceof KenjakuEntity
                           || entity instanceof LivingEntity _livEnt57 && _livEnt57.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                        : (
                              entity instanceof ServerPlayer _plr53
                                    && _plr53.m_9236_() instanceof ServerLevel
                                    && _plr53.m_8960_()
                                       .m_135996_(_plr53.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:mastery_open_barrier_type_domain")))
                                       .m_8193_()
                                 || entity instanceof LivingEntity _livEnt54 && _livEnt54.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                           )
                           && !entity.m_6144_()
                  )) {
                  entity.getPersistentData().m_128347_("cnt2", 1.0);
               } else if (!(entity instanceof FushiguroMegumiEntity) && !(entity instanceof FushiguroMegumiShibuyaEntity)) {
                  entity.getPersistentData().m_128347_("cnt2", 0.0);
               } else {
                  entity.getPersistentData().m_128347_("cnt2", -1.0);
               }

               reStart = false;
               update_pos = false;
               entity.getPersistentData().m_128379_("Failed", false);
               entity.getPersistentData().m_128379_("Cover", false);

               for (int index1 = 0; index1 < 2; index1++) {
                  entity.getPersistentData().m_128347_("x_pos_doma2", (double)Math.round(entity.m_20185_()));
                  entity.getPersistentData().m_128347_("y_pos_doma2", (double)Math.round(entity.m_20186_()));
                  entity.getPersistentData().m_128347_("z_pos_doma2", (double)Math.round(entity.m_20189_()));
                  Vec3 _center = new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_());

                  for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(domainRadius * 2.0 * 2.0 / 2.0), e -> true)) {
                     x_dis = entity.m_20185_() - entityiterator.m_20185_();
                     y_dis = entity.m_20186_() - entityiterator.m_20186_();
                     z_dis = entity.m_20189_() - entityiterator.m_20189_();
                     dis = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;
                     if (dis < domainRadius * domainRadius) {
                        if (entity != entityiterator && entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, (int)(domainRadius * 2.0 + 1.0), 9, false, false));
                        }

                        if ((
                              !(entityiterator instanceof LivingEntity _livEnt82)
                                 || !_livEnt82.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                           )
                           && entityiterator.getPersistentData().m_128459_("select") == 0.0
                           && entityiterator instanceof LivingEntity _entity
                           && !_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(
                              new MobEffectInstance(
                                 (MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get(),
                                 (int)(domainRadius * 2.0 + 20.0),
                                 (int)(entity.getPersistentData().m_128459_("select") + 10.0),
                                 false,
                                 false
                              )
                           );
                        }
                     }

                     if ((!(entity instanceof LivingEntity _livEnt86) || !_livEnt86.m_21023_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get()))
                        && (
                           entityiterator instanceof LivingEntity _livEnt87 && _livEnt87.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                              || entityiterator.getPersistentData().m_128459_("select") != 0.0
                        )) {
                        x_dis = entity.m_20185_() - entityiterator.getPersistentData().m_128459_("x_pos_doma");
                        y_dis = entity.m_20186_() - entityiterator.getPersistentData().m_128459_("y_pos_doma");
                        z_dis = entity.m_20189_() - entityiterator.getPersistentData().m_128459_("z_pos_doma");
                        dis = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;
                        if (dis < (domainRadius - 1.0) * (domainRadius - 1.0)) {
                           label366:
                           if (entity != entityiterator) {
                              label364: {
                                 label471: {
                                    if (entityiterator instanceof LivingEntity _livEnt96
                                       && _livEnt96.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                       int var131;
                                       label358: {
                                          if (entityiterator instanceof LivingEntity _livEnt
                                             && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                             var131 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                             break label358;
                                          }

                                          var131 = 0;
                                       }

                                       if (var131 <= 0) {
                                          break label364;
                                       }
                                       break label471;
                                    }

                                    if (entityiterator.getPersistentData().m_128459_("select") == 0.0
                                       || !(entityiterator.getPersistentData().m_128459_("cnt2") > 0.0)) {
                                       break label364;
                                    }
                                 }

                                 if (!update_pos) {
                                    entityiterator.getPersistentData().m_128347_("x_pos_doma", entity.getPersistentData().m_128459_("x_pos_doma"));
                                    entityiterator.getPersistentData().m_128347_("y_pos_doma", entity.getPersistentData().m_128459_("y_pos_doma"));
                                    entityiterator.getPersistentData().m_128347_("z_pos_doma", entity.getPersistentData().m_128459_("z_pos_doma"));
                                    update_pos = true;
                                    break label366;
                                 }
                              }

                              entity.getPersistentData().m_128347_("x_pos_doma", entityiterator.getPersistentData().m_128459_("x_pos_doma"));
                              entity.getPersistentData().m_128347_("y_pos_doma", entityiterator.getPersistentData().m_128459_("y_pos_doma"));
                              entity.getPersistentData().m_128347_("z_pos_doma", entityiterator.getPersistentData().m_128459_("z_pos_doma"));
                              update_pos = true;
                           }

                           if (entity != entityiterator) {
                              entity.getPersistentData().m_128379_("Failed", true);
                              if (entityiterator instanceof LivingEntity _livEnt114
                                 && _livEnt114.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                 int var132;
                                 label342: {
                                    if (entityiterator instanceof LivingEntity _livEnt
                                       && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                       var132 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                       break label342;
                                    }

                                    var132 = 0;
                                 }

                                 if (var132 == 0) {
                                    entity.getPersistentData().m_128347_("cnt5", 1.0);
                                 }
                              }

                              if (entityiterator instanceof LivingEntity _livEnt117
                                 && _livEnt117.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                                 && !entityiterator.getPersistentData().m_128471_("Cover")) {
                                 entityiterator.getPersistentData().m_128379_("Failed", true);
                              }
                           }
                        } else if (dis < domainRadius * 2.0 * domainRadius * 2.0 && !reStart && entity != entityiterator) {
                           label472: {
                              if (entityiterator instanceof LivingEntity _livEnt121
                                 && _livEnt121.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                 int var130;
                                 label371: {
                                    if (entityiterator instanceof LivingEntity _livEnt
                                       && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                       var130 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                       break label371;
                                    }

                                    var130 = 0;
                                 }

                                 if (var130 != 0) {
                                    continue;
                                 }
                                 break label472;
                              }

                              if (entityiterator.getPersistentData().m_128459_("select") == 0.0
                                 || !(entityiterator.getPersistentData().m_128459_("cnt2") <= 0.0)) {
                                 continue;
                              }
                           }

                           reStart = true;
                           x_pos = entityiterator.getPersistentData().m_128459_("x_pos_doma");
                           y_pos = entityiterator.getPersistentData().m_128459_("y_pos_doma");
                           z_pos = entityiterator.getPersistentData().m_128459_("z_pos_doma");
                        }
                     }
                  }

                  if (!reStart || entity.getPersistentData().m_128471_("Failed") || update_pos) {
                     break;
                  }

                  entity.m_6021_(x_pos, y_pos, z_pos);
                  if (entity instanceof ServerPlayer _serverPlayer) {
                     _serverPlayer.f_8906_.m_9774_(x_pos, y_pos, z_pos, entity.m_146908_(), entity.m_146909_());
                  }

                  entity.getPersistentData().m_128347_("x_pos_doma", x_pos);
                  entity.getPersistentData().m_128347_("y_pos_doma", y_pos);
                  entity.getPersistentData().m_128347_("z_pos_doma", z_pos);
                  update_pos = true;
               }

               Vec3 _center = new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_());

               for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(10.0), e -> true)) {
                  if (entity != entityiterator && !entityiterator.m_20147_()) {
                     x_dis = entityiterator.m_20185_() - entity.m_20185_();
                     y_dis = entityiterator.m_20186_() - entity.m_20186_();
                     z_dis = entityiterator.m_20189_() - entity.m_20189_();
                     dis = x_dis * x_dis + z_dis * z_dis;
                     if (dis > 0.0 && dis < 100.0) {
                        x_dis /= dis;
                        y_dis /= dis;
                        z_dis /= dis;
                        x_pos = entityiterator.m_20185_();
                        y_pos = entityiterator.m_20186_();
                        z_pos = entityiterator.m_20189_();
                        loop_num = Math.ceil(10.0 - dis);

                        for (int index2 = 0; index2 < (int)Math.round(loop_num); index2++) {
                           dis = (x_pos - entity.getPersistentData().m_128459_("x_pos_doma")) * (x_pos - entity.getPersistentData().m_128459_("x_pos_doma"))
                              + (y_pos - entity.getPersistentData().m_128459_("y_pos_doma")) * (y_pos - entity.getPersistentData().m_128459_("y_pos_doma"))
                              + (z_pos - entity.getPersistentData().m_128459_("z_pos_doma")) * (z_pos - entity.getPersistentData().m_128459_("z_pos_doma"));
                           if (dis > (MapVariables.get(world).DomainExpansionRadius - 3.0) * (MapVariables.get(world).DomainExpansionRadius - 3.0)) {
                              break;
                           }

                           x_pos += x_dis;
                           z_pos += z_dis;
                           if (world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()) {
                              y_pos++;
                           }
                        }

                        entityiterator.m_6021_(x_pos, y_pos, z_pos);
                        if (entityiterator instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_.m_9774_(x_pos, y_pos, z_pos, entityiterator.m_146908_(), entityiterator.m_146909_());
                        }
                     }
                  }
               }
            }

            if (entity instanceof LivingEntity _livEnt157 && _livEnt157.m_21023_((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get())) {
               if (entity.getPersistentData().m_128459_("cnt1") >= 5.0) {
                  yaw = Math.toRadians((double)(entity.m_146908_() + 90.0F));
                  pitch = Math.toRadians((double)entity.m_146909_());
                  distance = 0.5 * (double)entity.m_20205_();
                  x_pos = entity.m_20185_() + Math.cos(yaw) * Math.cos(pitch) * distance;
                  y_pos = entity.m_20186_() + (double)entity.m_20206_() * 0.9 + Math.sin(pitch) * -1.0 * distance;
                  z_pos = entity.m_20189_() + Math.sin(yaw) * Math.cos(pitch) * distance;
                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(
                        (SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_BLOOD_RED.get(),
                        x_pos,
                        y_pos,
                        z_pos,
                        (int)(2.0 + distance),
                        0.1 * distance,
                        0.1 * distance,
                        0.1 * distance,
                        0.1
                     );
                  }

                  if (entity.getPersistentData().m_128459_("cnt1") >= 20.0) {
                     if (world instanceof Level _level) {
                        if (!_level.m_5776_()) {
                           _level.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:crush")),
                              SoundSource.NEUTRAL,
                              (float)(0.5 * (1.0 + distance)),
                              1.1F
                           );
                        } else {
                           _level.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:crush")),
                              SoundSource.NEUTRAL,
                              (float)(0.5 * (1.0 + distance)),
                              1.1F,
                              false
                           );
                        }
                     }

                     if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 6000, 2, false, false));
                     }

                     entity.getPersistentData().m_128347_("select", 0.0);
                     entity.getPersistentData().m_128347_("skill", 0.0);
                     return;
                  }
               }

               return;
            }

            if (!entity.getPersistentData().m_128471_("Failed") || !(entity.getPersistentData().m_128459_("cnt5") > 0.0)) {
               old_failed = entity.getPersistentData().m_128471_("Failed");
               entity.getPersistentData().m_128379_("Failed", false);
               x_pos = entity.getPersistentData().m_128459_("x_pos_doma");
               y_pos = entity.getPersistentData().m_128459_("y_pos_doma");
               z_pos = entity.getPersistentData().m_128459_("z_pos_doma");
               entity.getPersistentData().m_128347_("cnt_cover", entity.getPersistentData().m_128459_("cnt1"));
               DomainExpansionBattleProcedure.execute(world, x_pos, y_pos, z_pos, entity);
               entity.getPersistentData().m_128379_("Failed", old_failed);
            } else if (entity.getPersistentData().m_128459_("cnt1") > 1.0) {
               entity.getPersistentData()
                  .m_128347_("cnt1", entity.getPersistentData().m_128459_("cnt1") < 34.0 ? 34.0 : Math.max(50.0, domainRadius * 2.0) + 5.0);
            }

            if (entity.getPersistentData().m_128459_("cnt1") >= Math.max(34.0, domainRadius * 2.0 + 1.0)) {
               double _setval = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new PlayerVariables()))
                     .PlayerCursePowerChange
                  + 200.0;
               entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                  capability.PlayerCursePowerChange = _setval;
                  capability.syncPlayerVariables(entity);
               });
               entity.getPersistentData().m_128379_("StartDomainAttack", true);
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(
                     new MobEffectInstance(
                        (MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get(),
                        entity.getPersistentData().m_128459_("select") == 29.0 ? 3600 : 1200,
                        (int)entity.getPersistentData().m_128459_("cnt2"),
                        true,
                        false
                     )
                  );
               }

               entity.getPersistentData().m_128347_("skill", 0.0);
               boolean _setvalx = true;
               entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                  capability.noChangeTechnique = _setval;
                  capability.syncPlayerVariables(entity);
               });
               KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
            }
         }
      }
   }
}
