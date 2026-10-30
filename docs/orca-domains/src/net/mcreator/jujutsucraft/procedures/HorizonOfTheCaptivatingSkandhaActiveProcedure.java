package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.entity.SeaSerpentEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;

public class HorizonOfTheCaptivatingSkandhaActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double old_skill = 0.0;
         double size = 0.0;
         double HP = 0.0;
         double num1 = 0.0;
         double range = 0.0;
         double old_cooldown = 0.0;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         range = MapVariables.get(world).DomainExpansionRadius * 2.0;
         if (!entity.getPersistentData().m_128471_("Failed")) {
            int var10000;
            label80: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label80;
               }

               var10000 = 0;
            }

            if (var10000 % 90 == 20) {
               int var10001;
               label74: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_(MobEffects.f_19600_)) {
                     var10001 = _livEnt.m_21124_(MobEffects.f_19600_).m_19564_();
                     break label74;
                  }

                  var10001 = 0;
               }

               HP = (double)(20 + var10001 * 5);
               old_skill = entity.getPersistentData().m_128459_("skill");
               old_cooldown = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
               entity.getPersistentData().m_128347_("skill", 809.0);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 125.0);

               for (int index0 = 0; index0 < 8; index0++) {
                  if (Math.random() < 0.5) {
                     num1 = Math.toRadians(Math.random() * 360.0);
                     x_pos = entity.getPersistentData().m_128459_("x_pos_doma") + Math.sin(num1) * (range / 2.0 - 4.0);
                     y_pos = entity.getPersistentData().m_128459_("y_pos_doma");
                     z_pos = entity.getPersistentData().m_128459_("z_pos_doma") + Math.cos(num1) * (range / 2.0 - 4.0);
                     size = 0.5 + Math.random();
                     if (world instanceof ServerLevel) {
                        ServerLevel _serverLevel = (ServerLevel)world;
                        Entity entityinstance = ((EntityType)JujutsucraftModEntities.SEA_SERPENT.get())
                           .m_262451_(_serverLevel, null, null, BlockPos.m_274561_(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
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

                           entityinstance.m_146922_((float)Math.toDegrees(num1));
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
                                 _livingEntity16.m_21051_(Attributes.f_22276_).m_22100_(HP);
                              }
                           }

                           if (entityinstance instanceof LivingEntity _entity) {
                              _entity.m_21153_((float)HP);
                           }

                           if (entityinstance instanceof LivingEntity) {
                              LivingEntity _livingEntity20 = (LivingEntity)entityinstance;
                              if (_livingEntity20.m_21204_().m_22171_((Attribute)JujutsucraftModAttributes.SIZE.get())) {
                                 _livingEntity20.m_21051_((Attribute)JujutsucraftModAttributes.SIZE.get()).m_22100_((5.0 + Math.random() * 7.5) * 0.1);
                              }
                           }

                           if (entityinstance instanceof SeaSerpentEntity animatable) {
                              animatable.setTexture("dagon_fish" + Math.round(Math.ceil(Math.random() * 3.0)));
                           }

                           _serverLevel.m_7967_(entityinstance);
                        }
                     }
                  }
               }

               entity.getPersistentData().m_128347_("skill", old_skill);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", old_cooldown);
            }
         }
      }
   }
}
