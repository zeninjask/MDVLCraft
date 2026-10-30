package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;

public class TakumaInoDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label72: {
            double old_skill = 0.0;
            double size = 0.0;
            double ticks = 0.0;
            double HP = 0.0;
            double num1 = 0.0;
            range = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label72;
            }

            var10000 = 0.0;
         }

         double var29 = var10000;
         if (!entity.getPersistentData().m_128471_("Failed") && var29 % 60.0 == 20.0) {
            int var10001;
            label64: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                  var10001 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                  break label64;
               }

               var10001 = 0;
            }

            double var30 = (double)(40 + var10001 * 4);
            double var27 = entity.getPersistentData().m_128459_("skill");
            double var33 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
            entity.getPersistentData().m_128347_("skill", 4005.0);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 75.0);

            for (int index0 = 0; index0 < 2; index0++) {
               if (Math.random() < 0.5) {
                  double var31 = Math.toRadians(Math.random() * 360.0);
                  double var34 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(var31) * (range / 2.0 - 4.0);
                  double var35 = entity.getPersistentData().m_128459_("y_pos_doma");
                  double var36 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(var31) * (range / 2.0 - 4.0);
                  double var28 = 0.5 + Math.random();
                  if (world instanceof ServerLevel) {
                     ServerLevel _serverLevel = (ServerLevel)world;
                     Entity entityinstance = ((EntityType)JujutsucraftModEntities.KAICHI.get())
                        .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(var34, var35, var36), MobSpawnType.MOB_SUMMONED, false, false);
                     if (entityinstance != null) {
                        entityinstance.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                        SetRangedAmmoProcedure.execute(entity, entityinstance);
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
                                 "data merge entity @s {NoAI:1b}"
                              );
                        }

                        entityinstance.m_146922_((float)Math.toDegrees(var31));
                        entityinstance.m_146926_(0.0F);
                        entityinstance.m_5618_(entityinstance.m_146908_());
                        entityinstance.m_5616_(entityinstance.m_146908_());
                        entityinstance.f_19859_ = entityinstance.m_146908_();
                        entityinstance.f_19860_ = entityinstance.m_146909_();
                        if (entityinstance instanceof LivingEntity _entity) {
                           _entity.f_20884_ = _entity.m_146908_();
                           _entity.f_20886_ = _entity.m_146908_();
                        }

                        if (entityinstance instanceof LivingEntity) {
                           LivingEntity _livingEntity16 = (LivingEntity)entityinstance;
                           if (_livingEntity16.m_21204_().m_22171_(Attributes.f_22276_)) {
                              _livingEntity16.m_21051_(Attributes.f_22276_).m_22100_(var30);
                           }
                        }

                        if (entityinstance instanceof LivingEntity _entity) {
                           _entity.m_21153_((float)var30);
                        }

                        entityinstance.getPersistentData().m_128347_("cnt3", 1.0);
                        _serverLevel.m_7967_(entityinstance);
                     }
                  }
               }
            }

            entity.getPersistentData().m_128347_("skill", var27);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var33);
         }
      }
   }
}
