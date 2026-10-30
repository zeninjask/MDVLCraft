package net.mcreator.jujutsucraft.procedures;

import java.util.Iterator;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.HigurumaHiromiEntity;
import net.mcreator.jujutsucraft.entity.KenjakuEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaCullingGameEntity;
import net.mcreator.jujutsucraft.entity.OkkotsuYutaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DomainExpansionOnEffectActiveTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         boolean failed = false;
         boolean logic_a = false;
         boolean logic_b = false;
         boolean noClosing = false;
         boolean update1 = false;
         boolean old_failed = false;
         boolean use_old = false;
         double range = 0.0;
         double level = 0.0;
         double tick = 0.0;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double tick_1 = 0.0;
         double tick_2 = 0.0;
         double distance = 0.0;
         double old_skill = 0.0;
         double domainPower1 = 0.0;
         double domainPower2 = 0.0;
         double str_lv = 0.0;
         double x_dis = 0.0;
         double y_dis = 0.0;
         double z_dis = 0.0;
         if (entity.m_6084_()) {
            double var10000;
            label593: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label593;
               }

               var10000 = 0.0;
            }

            label588: {
               tick_1 = var10000;
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                  var136 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                  break label588;
               }

               var136 = 0;
            }

            label583: {
               str_lv = (double)(var136 + 10);
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var137 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                  break label583;
               }

               var137 = 0;
            }

            if (var137 > 0) {
               str_lv *= 1.15;
            }

            if (entity.getPersistentData().m_128459_("select") == 27.0 || entity.getPersistentData().m_128459_("skill_domain") == 27.0) {
               str_lv *= 1.5;
            } else if (entity.getPersistentData().m_128459_("select") == 29.0 || entity.getPersistentData().m_128459_("skill_domain") == 29.0) {
               str_lv *= 2.0;
            }

            if (entity.getPersistentData().m_128459_("skill_domain") == 0.0 && entity.getPersistentData().m_128459_("skill") == 0.0) {
               entity.getPersistentData().m_128347_("skill_domain", entity.getPersistentData().m_128459_("select"));
               entity.getPersistentData().m_128347_("select", 0.0);
               update1 = true;
            }

            if (entity.getPersistentData().m_128459_("skill_domain") > 0.0) {
               if (!update1) {
                  DomainActiveProcedure.execute(world, x, y, z, entity);
               }

               int var10001;
               label566: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                     var10001 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                     break label566;
                  }

                  var10001 = 0;
               }

               range = MapVariables.get(world).DomainExpansionRadius * (double)(var10001 > 0 ? 18 : 2);
            }

            old_skill = entity.getPersistentData().m_128459_("skill");
            entity.getPersistentData().m_128347_("skill", 0.0);
            if (tick_1 % 10.0 == 0.0 || update1) {
               logic_a = false;
               Vec3 _center = new Vec3(
                  entity.getPersistentData().m_128459_("x_pos_doma"),
                  entity.getPersistentData().m_128459_("y_pos_doma"),
                  entity.getPersistentData().m_128459_("z_pos_doma")
               );

               for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                  x_dis = entity.getPersistentData().m_128459_("x_pos_doma") - entityiterator.m_20185_();
                  y_dis = entity.getPersistentData().m_128459_("y_pos_doma") - (entityiterator.m_20186_() + (double)entityiterator.m_20206_() * 0.5);
                  z_dis = entity.getPersistentData().m_128459_("z_pos_doma") - entityiterator.m_20189_();
                  distance = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;
                  if (distance < range * 0.5 * range * 0.5) {
                     if (entity == entityiterator) {
                        logic_a = true;
                     } else {
                        label536: {
                           if (entityiterator instanceof LivingEntity _livEnt28 && _livEnt28.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())
                              )
                            {
                              label531: {
                                 if (entityiterator instanceof LivingEntity _livEnt
                                    && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                                    var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19564_();
                                    break label531;
                                 }

                                 var10000 = 0.0;
                              }

                              label526: {
                                 level = var10000;
                                 if (entityiterator instanceof LivingEntity _livEnt
                                    && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                                    var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19557_();
                                    break label526;
                                 }

                                 var10000 = 0.0;
                              }

                              tick = var10000;
                              tick -= (double)Math.round(Math.sqrt(str_lv + 1.0) * 10.0);
                              if (entityiterator instanceof LivingEntity _entity) {
                                 _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
                              }

                              if (level >= 0.0 && entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                                 _entity.m_7292_(
                                    new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), (int)tick, (int)level, true, true)
                                 );
                              }
                              break label536;
                           }

                           if (tick_1 % 20.0 == 0.0) {
                              EffectCharactorProcedure.execute(world, entity, entityiterator);
                           }
                        }

                        if (entityiterator instanceof LivingEntity) {
                           LivingEntity _livEnt33 = (LivingEntity)entityiterator;
                           if (_livEnt33.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                              continue;
                           }
                        }

                        if (entityiterator.getPersistentData().m_128459_("select") == 0.0 && entityiterator instanceof LivingEntity) {
                           LivingEntity _entity = (LivingEntity)entityiterator;
                           if (!_entity.m_9236_().m_5776_()) {
                              _entity.m_7292_(
                                 new MobEffectInstance(
                                    (MobEffect)JujutsucraftModMobEffects.NEUTRALIZATION.get(),
                                    20,
                                    (int)(entity.getPersistentData().m_128459_("skill_domain") + 10.0),
                                    false,
                                    false
                                 )
                              );
                           }
                        }
                     }
                  }
               }

               if (!logic_a && entity.getPersistentData().m_128459_("skill_domain") > 0.0) {
                  label550: {
                     if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                        var140 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                        break label550;
                     }

                     var140 = 0;
                  }

                  if (var140 == 0 && entity instanceof LivingEntity _entity) {
                     _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                  }
               }
            }

            if (entity.getPersistentData().m_128459_("oldHealth") != (double)(entity instanceof LivingEntity _livEnt ? _livEnt.m_21223_() : -1.0F)) {
               if (entity.getPersistentData().m_128459_("oldHealth") > (double)(entity instanceof LivingEntity _livEntx ? _livEntx.m_21223_() : -1.0F)) {
                  entity.getPersistentData()
                     .m_128347_(
                        "totalDamage",
                        entity.getPersistentData().m_128459_("totalDamage")
                           + entity.getPersistentData().m_128459_("oldHealth")
                           - (double)(entity instanceof LivingEntity _livEntxx ? _livEntxx.m_21223_() : -1.0F)
                     );
               } else {
                  entity.getPersistentData()
                     .m_128347_(
                        "totalDamage",
                        entity.getPersistentData().m_128459_("totalDamage")
                           + (
                                 entity.getPersistentData().m_128459_("oldHealth")
                                    - (double)(entity instanceof LivingEntity _livEntx ? _livEntx.m_21223_() : -1.0F)
                              )
                              * 0.5
                     );
               }
            }

            entity.getPersistentData().m_128347_("oldHealth", entity instanceof LivingEntity _livEntx ? (double)_livEntx.m_21223_() : -1.0);
            if ((tick_1 % 20.0 == 0.0 || update1) && entity.getPersistentData().m_128459_("select") == 0.0) {
               domainPower1 = str_lv
                  * (
                     Math.max(
                           (double)Math.max(entity instanceof LivingEntity _livEntxxx ? _livEntxxx.m_21233_() : -1.0F, 1.0F)
                              - Math.max(entity.getPersistentData().m_128459_("totalDamage"), 0.0) * 2.0,
                           0.0
                        )
                        / (double)Math.max(entity instanceof LivingEntity _livEntxx ? _livEntxx.m_21233_() : -1.0F, 1.0F)
                  )
                  * Math.min(Math.min(tick_1, 1200.0) / 2400.0 + 0.5, 1.0);
               failed = false;
               logic_a = false;
               logic_b = false;

               for (int index0 = 0; index0 < 2; index0++) {
                  Vec3 _center = new Vec3(
                     entity.getPersistentData().m_128459_("x_pos_doma"),
                     entity.getPersistentData().m_128459_("y_pos_doma"),
                     entity.getPersistentData().m_128459_("z_pos_doma")
                  );

                  for (Entity entityiteratorx : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
                     label384: {
                        if (entity instanceof LivingEntity _livEntxxxx && _livEntxxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                           var146 = _livEntxxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                           break label384;
                        }

                        var146 = 0;
                     }

                     label394: {
                        if (var146 > 0) {
                           label391: {
                              if (entityiteratorx instanceof LivingEntity _livEntxxxx
                                 && _livEntxxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                 var147 = _livEntxxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                 break label391;
                              }

                              var147 = 0;
                           }

                           if (var147 == 0) {
                              var148 = true;
                              break label394;
                           }
                        }

                        var148 = false;
                     }

                     noClosing = var148;
                     if (entity != entityiteratorx
                        && (
                           entityiteratorx instanceof LivingEntity _livEnt65 && _livEnt65.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                              || entityiteratorx.getPersistentData().m_128459_("select") != 0.0
                        )) {
                        x_dis = entity.getPersistentData().m_128459_("x_pos_doma") - entityiteratorx.m_20185_();
                        y_dis = entity.getPersistentData().m_128459_("y_pos_doma") - (entityiteratorx.m_20186_() + (double)entityiteratorx.m_20206_() * 0.5);
                        z_dis = entity.getPersistentData().m_128459_("z_pos_doma") - entityiteratorx.m_20189_();
                        distance = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;
                        if (distance < range * 0.5 * range * 0.5) {
                           label414: {
                              if (entityiteratorx instanceof LivingEntity _livEntxxxx && _livEntxxxx.m_21023_(MobEffects.f_19600_)) {
                                 var149 = _livEntxxxx.m_21124_(MobEffects.f_19600_).m_19564_();
                                 break label414;
                              }

                              var149 = 0;
                           }

                           label419: {
                              domainPower2 = (double)(var149 + 10);
                              if (entityiteratorx instanceof LivingEntity _livEntxxxx
                                 && _livEntxxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                 var150 = _livEntxxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                 break label419;
                              }

                              var150 = 0;
                           }

                           if (var150 > 0) {
                              domainPower2 *= 1.15;
                           }

                           if (entityiteratorx.getPersistentData().m_128459_("select") == 27.0
                              || entityiteratorx.getPersistentData().m_128459_("skill_domain") == 27.0) {
                              domainPower2 *= 1.5;
                           } else if (entityiteratorx.getPersistentData().m_128459_("select") == 29.0
                              || entityiteratorx.getPersistentData().m_128459_("skill_domain") == 29.0) {
                              domainPower2 *= 2.0;
                           }

                           if (entityiteratorx.getPersistentData().m_128459_("select") != 0.0) {
                              tick_2 = 1200.0;
                           } else {
                              label436: {
                                 if (entityiteratorx instanceof LivingEntity _livEntxxxx
                                    && _livEntxxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                    var10000 = (double)_livEntxxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                                    break label436;
                                 }

                                 var10000 = 0.0;
                              }

                              tick_2 = var10000;
                              domainPower2 = domainPower2
                                 * (
                                    Math.max(
                                          (double)Math.max(entityiteratorx instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.m_21233_() : -1.0F, 1.0F)
                                             - Math.max(entityiteratorx.getPersistentData().m_128459_("totalDamage"), 0.0) * 2.0,
                                          0.0
                                       )
                                       / (double)Math.max(entityiteratorx instanceof LivingEntity _livEntxxxx ? _livEntxxxx.m_21233_() : -1.0F, 1.0F)
                                 )
                                 * Math.min(Math.min(tick_2, 1200.0) / 2400.0 + 0.5, 1.0);
                           }

                           label490: {
                              if (!(domainPower1 - domainPower2 >= 10.0)) {
                                 if (!logic_a) {
                                    break label490;
                                 }

                                 label486: {
                                    if (entityiteratorx instanceof LivingEntity _livEntxxxx
                                       && _livEntxxxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                       var152 = _livEntxxxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                                       break label486;
                                    }

                                    var152 = 0;
                                 }

                                 if (var152 > 0) {
                                    break label490;
                                 }
                              }

                              if (entityiteratorx instanceof LivingEntity _livEnt86
                                 && _livEnt86.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                                 failed = false;
                                 logic_b = true;
                                 entityiteratorx.getPersistentData().m_128379_("Failed", false);
                                 entityiteratorx.getPersistentData().m_128379_("DomainDefeated", !noClosing);
                                 if ((
                                       !(entityiteratorx instanceof LivingEntity _livEnt89)
                                          || !_livEnt89.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                                    )
                                    && entityiteratorx instanceof LivingEntity _entity
                                    && !_entity.m_9236_().m_5776_()) {
                                    _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get(), 5, 0, false, false));
                                 }

                                 if (entityiteratorx instanceof LivingEntity _entity) {
                                    _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                                 }
                                 continue;
                              }
                           }

                           if (noClosing
                              && (tick_1 <= tick_2 ? tick_2 < 1000.0 : tick_1 < 1000.0)
                              && entityiteratorx instanceof LivingEntity _livEnt92
                              && _livEnt92.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                              label637: {
                                 logic_a = true;
                                 label467:
                                 if (entityiteratorx instanceof Player
                                    ? !(entityiteratorx instanceof ServerPlayer _plr94)
                                       || !(_plr94.m_9236_() instanceof ServerLevel)
                                       || !_plr94.m_8960_()
                                          .m_135996_(_plr94.f_8924_.m_129889_().m_136041_(new ResourceLocation("jujutsucraft:anti_open_barrier_type_domain")))
                                          .m_8193_()
                                    : !(entityiteratorx.getPersistentData().m_128459_("cnt_learn_domain") > 0.0)) {
                                    if (entityiteratorx instanceof LivingEntity _livEnt96
                                       && _livEnt96.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                                       break label467;
                                    }

                                    if (!(entityiteratorx instanceof GojoSatoruEntity)
                                       && !(entityiteratorx instanceof HigurumaHiromiEntity)
                                       && !(entityiteratorx instanceof OkkotsuYutaEntity)
                                       && !(entityiteratorx instanceof OkkotsuYutaCullingGameEntity)
                                       && !(entityiteratorx instanceof KenjakuEntity)) {
                                       if (!(entityiteratorx instanceof Player) || !(entityiteratorx instanceof ServerPlayer _player)) {
                                          break label637;
                                       }

                                       Advancement _adv = _player.f_8924_
                                          .m_129889_()
                                          .m_136041_(new ResourceLocation("jujutsucraft:anti_open_barrier_type_domain"));
                                       AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                                       if (_ap.m_8193_()) {
                                          break label637;
                                       }

                                       Iterator var61 = _ap.m_8219_().iterator();

                                       while (true) {
                                          if (!var61.hasNext()) {
                                             break label637;
                                          }

                                          String criteria = (String)var61.next();
                                          _player.m_8960_().m_135988_(_adv, criteria);
                                       }
                                    }

                                    entityiteratorx.getPersistentData().m_128347_("cnt_learn_domain", 1.0);
                                    break label637;
                                 }

                                 logic_a = false;
                              }

                              if (logic_a) {
                                 break;
                              }
                           }

                           if (!logic_b) {
                              if (distance < MapVariables.get(world).DomainExpansionRadius) {
                                 entity.getPersistentData().m_128379_("Failed", true);
                              } else {
                                 use_old = true;
                                 old_failed = entity.getPersistentData().m_128471_("Failed");
                                 entity.getPersistentData().m_128379_("Failed", true);
                              }

                              failed = true;
                           }
                        }
                     }
                  }

                  if (!logic_a) {
                     break;
                  }
               }
            } else {
               failed = true;
            }

            entity.getPersistentData().m_128347_("skill", old_skill);
            if (!entity.getPersistentData().m_128471_("Failed") && !entity.getPersistentData().m_128471_("Cover")) {
               label344: {
                  if (entity instanceof LivingEntity _livEntxx && _livEntxx.m_21023_((MobEffect)JujutsucraftModMobEffects.ZONE.get())) {
                     var154 = _livEntxx.m_21124_((MobEffect)JujutsucraftModMobEffects.ZONE.get()).m_19557_();
                     break label344;
                  }

                  var154 = 0;
               }

               if (var154 < 10 && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.ZONE.get(), 10, 0, false, false));
               }
            } else {
               label369:
               if (!failed && !entity.getPersistentData().m_128471_("Cover")) {
                  x_dis = entity.getPersistentData().m_128459_("x_pos_doma") - entity.m_20185_();
                  y_dis = entity.getPersistentData().m_128459_("y_pos_doma") - (entity.m_20186_() + (double)entity.m_20206_() * 0.5);
                  z_dis = entity.getPersistentData().m_128459_("z_pos_doma") - entity.m_20189_();
                  distance = x_dis * x_dis + y_dis * y_dis + z_dis * z_dis;
                  if (!(distance < MapVariables.get(world).DomainExpansionRadius * MapVariables.get(world).DomainExpansionRadius)) {
                     label364: {
                        if (entity instanceof LivingEntity _livEntxx && _livEntxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                           var153 = _livEntxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                           break label364;
                        }

                        var153 = 0;
                     }

                     if (var153 == 0) {
                        if (entity instanceof LivingEntity _entity) {
                           _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                        break label369;
                     }
                  }

                  entity.getPersistentData().m_128347_("cnt_cover", 1.0);
                  old_failed = false;
                  entity.getPersistentData().m_128379_("Failed", false);
                  entity.getPersistentData().m_128379_("Cover", true);
                  if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     MobEffectInstance var162;
                     MobEffect var169;
                     int var170;
                     int var10005;
                     label355: {
                        var162 = new MobEffectInstance;
                        var169 = (MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get();
                        var170 = (int)(tick_1 + 100.0);
                        if (entity instanceof LivingEntity _livEntxx && _livEntxx.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                           var10005 = _livEntxx.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                           break label355;
                        }

                        var10005 = 0;
                     }

                     var162./* $VF: Unable to resugar constructor */<init>(var169, var170, var10005, true, false);
                     _entity.m_7292_(var162);
                  }

                  entity.getPersistentData().m_128347_("x_pos_doma2", (double)Math.round(entity.m_20185_()));
                  entity.getPersistentData().m_128347_("y_pos_doma2", (double)Math.round(entity.m_20186_()));
                  entity.getPersistentData().m_128347_("z_pos_doma2", (double)Math.round(entity.m_20189_()));
               }

               if (entity.getPersistentData().m_128471_("Cover")) {
                  x_pos = entity.getPersistentData().m_128459_("x_pos_doma");
                  y_pos = entity.getPersistentData().m_128459_("y_pos_doma");
                  z_pos = entity.getPersistentData().m_128459_("z_pos_doma");
               } else {
                  x_pos = entity.m_20185_();
                  y_pos = entity.m_20186_();
                  z_pos = entity.m_20189_();
               }

               old_skill = entity.getPersistentData().m_128459_("cnt1");
               entity.getPersistentData().m_128347_("cnt1", 0.0);
               DomainExpansionBattleProcedure.execute(world, x_pos, y_pos, z_pos, entity);
               entity.getPersistentData().m_128347_("cnt1", old_skill);
               if (use_old) {
                  entity.getPersistentData().m_128379_("Failed", old_failed);
               }

               if (entity.getPersistentData().m_128471_("Cover")) {
                  entity.getPersistentData().m_128347_("cnt_cover", entity.getPersistentData().m_128459_("cnt_cover") + 1.0);
                  if (entity.getPersistentData().m_128459_("cnt_cover") > MapVariables.get(world).DomainExpansionRadius * 2.0 + 1.0) {
                     entity.getPersistentData().m_128379_("Cover", false);
                  }
               }
            }

            if (!entity.getPersistentData().m_128471_("Cover")) {
               if (entity instanceof Player) {
                  if (tick_1 % 20.0 == 0.0) {
                     entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                        capability.PlayerCursePowerChange -= 20.0;
                        if (capability.PlayerCursePower + capability.PlayerCursePowerChange <= 0.0 && entity instanceof LivingEntity _entityx) {
                           _entityx.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                        }
                     });
                  }
               } else if (entity.getPersistentData().m_128459_("cnt_target") > 5.0) {
                  entity.getPersistentData().m_128347_("cnt_domain_cancel", 0.0);
               } else {
                  short var163;
                  label335: {
                     entity.getPersistentData().m_128347_("cnt_domain_cancel", entity.getPersistentData().m_128459_("cnt_domain_cancel") + 1.0);
                     var10000 = entity.getPersistentData().m_128459_("cnt_domain_cancel");
                     if (entity instanceof LivingEntity _livEnt158 && _livEnt158.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                        var163 = 600;
                        break label335;
                     }

                     var163 = 100;
                  }

                  if (var10000 > (double)var163 && entity instanceof LivingEntity _entity) {
                     _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
                  }
               }
            }
         } else if (entity instanceof LivingEntity _entity) {
            _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
         }
      }
   }
}
