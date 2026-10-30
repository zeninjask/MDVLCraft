package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChimeraShadowGardenActiveProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double x_pos;
         double z_pos;
         double loop_num;
         double y_pos;
         boolean breakLoop;
         double range;
         double var10000;
         label261: {
            double distance = 0.0;
            double ticks = 0.0;
            double z_center = 0.0;
            range = 0.0;
            double rad1 = 0.0;
            x_pos = 0.0;
            z_pos = 0.0;
            double y_center = 0.0;
            double x_center = 0.0;
            loop_num = 0.0;
            y_pos = 0.0;
            breakLoop = false;
            boolean logic_hit = false;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label261;
            }

            var10000 = 0.0;
         }

         double var39 = var10000;
         double var44 = entity.getPersistentData().m_128459_("x_pos_doma");
         double var43 = entity.getPersistentData().m_128459_("y_pos_doma");
         double var40 = entity.getPersistentData().m_128459_("z_pos_doma");
         if (entity.getPersistentData().m_128471_("Failed")) {
            if (entity.m_20096_() && world instanceof ServerLevel _level) {
               _level.m_8767_(ParticleTypes.f_123765_, x, y, z, 20, 1.0, 0.0, 1.0, 0.05);
            }
         } else {
            double var42 = Math.toRadians(90.0 * (var39 % 4.0));
            double var38 = 4.0 * var39 % (range * 0.5 - 5.0);

            for (int index0 = 0; index0 < 5; index0++) {
               for (int index1 = 0; index1 < 72; index1++) {
                  if (!(Math.random() < 0.7)) {
                     var42 += Math.toRadians(10.0 * Math.random());
                     x_pos = var44 + Math.cos(var42) * var38;
                     y_pos = var43 + 0.25;
                     z_pos = var40 + Math.sin(var42) * var38;
                     if (world instanceof ServerLevel _level) {
                        _level.m_8767_(ParticleTypes.f_123765_, x_pos, y_pos, z_pos, 1, 0.2, 0.0, 0.2, 0.05);
                     }
                  }
               }

               var38++;
            }
         }

         if (entity.getPersistentData().m_128459_("friend_num") == 0.0) {
            entity.getPersistentData().m_128347_("friend_num", Math.random());
         }

         if (!entity.getPersistentData().m_128471_("Failed")) {
            boolean var45 = false;
            Vec3 _center = new Vec3(var44, var43, var40);

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
               if (entity != entityiterator && LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 10, 5, false, false));
                     }
                  }

                  if (entityiterator instanceof LivingEntity _entity) {
                     _entity.m_21195_(MobEffects.f_19596_);
                  }

                  if (entityiterator instanceof LivingEntity _entity) {
                     _entity.m_21195_(MobEffects.f_19603_);
                  }

                  if (world.m_8055_(BlockPos.m_274561_(entityiterator.m_20185_(), entityiterator.m_20186_() - 0.0, entityiterator.m_20189_())).m_60734_()
                        == JujutsucraftModBlocks.BLOCK_UNIVERSE.get()
                     || world.m_8055_(BlockPos.m_274561_(entityiterator.m_20185_(), entityiterator.m_20186_() - 1.0, entityiterator.m_20189_())).m_60734_()
                        == JujutsucraftModBlocks.BLOCK_UNIVERSE.get()
                     || world.m_8055_(BlockPos.m_274561_(entityiterator.m_20185_(), entityiterator.m_20186_() - 2.0, entityiterator.m_20189_())).m_60734_()
                        == JujutsucraftModBlocks.BLOCK_UNIVERSE.get()
                     || world.m_8055_(BlockPos.m_274561_(entityiterator.m_20185_(), entityiterator.m_20186_() - 3.0, entityiterator.m_20189_())).m_60734_()
                        == JujutsucraftModBlocks.BLOCK_UNIVERSE.get()) {
                     entityiterator.m_20256_(new Vec3(0.0, Math.min(entityiterator.m_20184_().m_7098_(), 0.0), 0.0));
                     if (!world.m_8055_(BlockPos.m_274561_(entityiterator.m_20185_(), entityiterator.m_20186_() - 0.5, entityiterator.m_20189_())).m_60815_()) {
                        entityiterator.m_6021_(entityiterator.m_20185_(), entityiterator.m_20186_() - 0.5, entityiterator.m_20189_());
                        if (entityiterator instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_
                              .m_9774_(
                                 entityiterator.m_20185_(),
                                 entityiterator.m_20186_() - 0.5,
                                 entityiterator.m_20189_(),
                                 entityiterator.m_146908_(),
                                 entityiterator.m_146909_()
                              );
                        }
                     }
                  }

                  if (!var45 || Math.random() < 0.5) {
                     var45 = true;
                     var44 = entityiterator.m_20185_();
                     var43 = entityiterator.m_20186_();
                     var40 = entityiterator.m_20189_();
                  }
               }
            }

            if (var39 % 40.0 == 0.0) {
               for (int index2 = 0; index2 < 2; index2++) {
                  if (!var45) {
                     var44 = entity.getPersistentData().m_128459_("x_pos_doma") + (Math.random() - 0.5) * range * 0.5;
                     var43 = entity.getPersistentData().m_128459_("y_pos_doma") + Math.random() * 0.1;
                     var40 = entity.getPersistentData().m_128459_("z_pos_doma") + (Math.random() - 0.5) * range * 0.5;
                  }

                  for (int index3 = 0; index3 < 128; index3++) {
                     x_pos = var44 + (Math.random() - 0.5) * 8.0;
                     y_pos = var43;
                     z_pos = var40 + (Math.random() - 0.5) * 8.0;
                     if (Math.sqrt(
                           Math.pow(x_pos - entity.getPersistentData().m_128459_("x_pos_doma"), 2.0)
                              + Math.pow(var43 - entity.getPersistentData().m_128459_("y_pos_doma"), 2.0)
                              + Math.pow(z_pos - entity.getPersistentData().m_128459_("z_pos_doma"), 2.0)
                        )
                        < range * 0.5 - 4.0) {
                        break;
                     }
                  }

                  if (var39 % 160.0 == 0.0) {
                     x_pos = var44;
                     z_pos = var40;

                     for (int index4 = 0; index4 < (int)(range / 2.0); index4++) {
                        y_pos++;
                        if (world.m_8055_(BlockPos.m_274561_(x_pos, y_pos, z_pos)).m_60815_()
                           || world.m_8055_(BlockPos.m_274561_(x_pos, y_pos + 1.0, z_pos)).m_60815_()
                           || world.m_8055_(BlockPos.m_274561_(x_pos + 0.0, y_pos + 1.0, z_pos + 1.0)).m_60815_()
                           || world.m_8055_(BlockPos.m_274561_(x_pos + 1.0, y_pos + 1.0, z_pos + 0.0)).m_60815_()
                           || world.m_8055_(BlockPos.m_274561_(x_pos + 0.0, y_pos + 1.0, z_pos + -1.0)).m_60815_()
                           || world.m_8055_(BlockPos.m_274561_(x_pos + -1.0, y_pos + 1.0, z_pos + 0.0)).m_60815_()) {
                           break;
                        }
                     }

                     if (world instanceof ServerLevel _serverLevel) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.MAX_ELEPHANT.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           ChimeraShadowGardenSetProcedure.execute(entity, entityinstance);
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

                           _serverLevel.m_7967_(entityinstance);
                        }
                     }

                     breakLoop = true;
                  } else if (var39 % 120.0 == 0.0) {
                     for (int index5 = 0;
                        index5 < 128
                           && !(
                              Math.sqrt(
                                    Math.pow(x_pos - entity.getPersistentData().m_128459_("x_pos_doma"), 2.0)
                                       + Math.pow(y_pos - entity.getPersistentData().m_128459_("y_pos_doma"), 2.0)
                                       + Math.pow(z_pos - entity.getPersistentData().m_128459_("z_pos_doma"), 2.0)
                                 )
                                 < range * 0.5 - 4.0
                           );
                        index5++
                     ) {
                        x_pos = var44 + (Math.random() - 0.5) * 8.0;
                        y_pos = var43;
                        z_pos = var40 + (Math.random() - 0.5) * 8.0;
                     }

                     if (loop_num == 0.0) {
                        if (world instanceof ServerLevel _serverLevelx) {
                           Entity entityinstance = ((EntityType)JujutsucraftModEntities.DIVINE_DOG_BLACK.get())
                              .m_262451_(_serverLevelx, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                           if (entityinstance != null) {
                              entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                              ChimeraShadowGardenSetProcedure.execute(entity, entityinstance);
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

                              _serverLevelx.m_7967_(entityinstance);
                           }
                        }
                     } else if (world instanceof ServerLevel _serverLevelxx) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.DIVINE_DOG_WHITE.get())
                           .m_262451_(_serverLevelxx, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           ChimeraShadowGardenSetProcedure.execute(entity, entityinstance);
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

                           _serverLevelxx.m_7967_(entityinstance);
                        }
                     }
                  } else if (var39 % 80.0 != 0.0) {
                     if (var39 % 40.0 == 0.0) {
                        x_pos = var44;
                        z_pos = var40;
                        breakLoop = true;
                        if (world instanceof ServerLevel _serverLevelxxx) {
                           Entity entityinstance = ((EntityType)JujutsucraftModEntities.TOAD_2.get())
                              .m_262451_(_serverLevelxxx, null, null, BlockPos.m_274561_(var44, y_pos, var40), MobSpawnType.MOB_SUMMONED, false, false);
                           if (entityinstance != null) {
                              entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                              ChimeraShadowGardenSetProcedure.execute(entity, entityinstance);
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

                              _serverLevelxxx.m_7967_(entityinstance);
                           }
                        }
                     }
                  } else {
                     y_pos += 4.0;

                     for (int index6 = 0;
                        index6 < 128
                           && !(
                              Math.sqrt(
                                    Math.pow(x_pos - entity.getPersistentData().m_128459_("x_pos_doma"), 2.0)
                                       + Math.pow(y_pos - entity.getPersistentData().m_128459_("y_pos_doma"), 2.0)
                                       + Math.pow(z_pos - entity.getPersistentData().m_128459_("z_pos_doma"), 2.0)
                                 )
                                 < range * 0.5 - 4.0
                           );
                        index6++
                     ) {
                        x_pos = var44 + (Math.random() - 0.5) * 8.0;
                        y_pos = var43;
                        z_pos = var40 + (Math.random() - 0.5) * 8.0;
                     }

                     if (world instanceof ServerLevel _serverLevelxxxx) {
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.NUE.get())
                           .m_262451_(_serverLevelxxxx, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                        if (entityinstance != null) {
                           entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                           ChimeraShadowGardenSetProcedure.execute(entity, entityinstance);
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

                           _serverLevelxxxx.m_7967_(entityinstance);
                        }
                     }
                  }

                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123765_, x_pos, y_pos, z_pos, 20, 0.5, 0.5, 0.5, 0.1);
                  }

                  loop_num++;
                  if (breakLoop) {
                     break;
                  }
               }
            }
         }
      }
   }
}
