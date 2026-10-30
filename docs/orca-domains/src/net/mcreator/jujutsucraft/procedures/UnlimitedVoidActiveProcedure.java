package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class UnlimitedVoidActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double rad;
         double range;
         double var10000;
         label145: {
            boolean logic_a = false;
            double distance = 0.0;
            double ticks = 0.0;
            range = 0.0;
            double rad1 = 0.0;
            double x_pos = 0.0;
            double z_pos = 0.0;
            double yaw = 0.0;
            double dis = 0.0;
            rad = 0.0;
            double rad_now = 0.0;
            double num1 = 0.0;
            double y_pos = 0.0;
            double num2 = 0.0;
            double x_center = 0.0;
            double y_center = 0.0;
            double z_center = 0.0;
            double y_power = 0.0;
            double z_power = 0.0;
            double x_power = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label145;
            }

            var10000 = 0.0;
         }

         double var48 = var10000;
         double var66 = entity.getPersistentData().m_128459_("x_pos_doma");
         double var67 = entity.getPersistentData().m_128459_("y_pos_doma");
         double var68 = entity.getPersistentData().m_128459_("z_pos_doma");
         if ((var48 % 5.0 == 0.0 || entity.getPersistentData().m_128471_("StartDomainAttack")) && !entity.getPersistentData().m_128471_("Failed")) {
            entity.getPersistentData().m_128379_("StartDomainAttack", false);
            entity.getPersistentData().m_128379_("onlyLiving", true);
            Vec3 _center = new Vec3(var66, var67, var68);

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
               if (entity != entityiterator && LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                  double var60;
                  if (entityiterator.getPersistentData().m_128471_("CursedSpirit")) {
                     var60 = 150.0;
                  } else {
                     var60 = 350.0;
                     if (entityiterator instanceof LivingEntity) {
                        LivingEntity _entity = (LivingEntity)entityiterator;
                        if (!_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), 6000, 4, false, false));
                        }
                     }
                  }

                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, (int)var60, 99, false, false));
                     }
                  }

                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19610_, (int)var60, 99, false, false));
                     }
                  }

                  if (!entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:ranged_ammo")))) {
                     if (entityiterator instanceof LivingEntity) {
                        LivingEntity _entity = (LivingEntity)entityiterator;
                        if (!_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance(MobEffects.f_19613_, (int)var60, 99, false, false));
                        }
                     }

                     if (entityiterator.getPersistentData().m_128459_("skill") != -999.0 && entityiterator instanceof LivingEntity _entity) {
                        _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
                     }

                     entityiterator.getPersistentData().m_128347_("skill", -999.0);
                     if (entityiterator instanceof LivingEntity) {
                        LivingEntity _entity = (LivingEntity)entityiterator;
                        if (!_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), (int)var60, 0, false, false));
                        }
                     }

                     if (entityiterator instanceof LivingEntity) {
                        LivingEntity _entity = (LivingEntity)entityiterator;
                        if (!_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), (int)var60, 0, false, false));
                        }
                     }

                     entityiterator.getPersistentData().m_128347_("skill", -999.0);
                  }
               }
            }

            entity.getPersistentData().m_128379_("onlyLiving", false);
            if (Math.random() < 0.1) {
               double var61 = Math.toRadians(Math.random() * 360.0);
               double var65 = range * 0.5 - 6.0;
               double var51 = var66 + Math.sin(var61) * var65;
               double var62 = var67 + 2.0;
               double var54 = var68 + Math.cos(var61) * var65;
               if (world instanceof ServerLevel _level) {
                  _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_UNLIMITED_VOID.get(), var51, var62, var54, 10, 1.0, 1.0, 1.0, 0.0);
               }
            }
         }

         double var58 = range * 0.5 - 8.0;
         double var57 = Math.toRadians(Math.random() * 360.0);
         double var59 = Math.toRadians((Math.random() - 0.5) * 180.0);
         boolean var46 = Math.random() < 0.5;

         for (int index0 = 0; index0 < 72; index0++) {
            if (Math.random() < 0.25) {
               double var52;
               double var55;
               double var63;
               if (var46) {
                  var52 = var66 + Math.cos(rad) * (Math.cos(var59) + Math.abs(Math.sin(var57) * Math.sin(var59))) * var58;
                  var63 = var67 + Math.abs(Math.sin(rad) * Math.sin(var59) * -1.0 * var58);
                  var55 = var68 + Math.sin(rad) * (Math.cos(var59) + Math.abs(Math.cos(var57) * Math.sin(var59))) * var58;
               } else {
                  var52 = var66 + Math.cos(rad) * Math.cos(var59) * var58;
                  var63 = var67 + Math.abs(Math.sin(rad) * var58);
                  var55 = var68 + Math.cos(rad) * Math.sin(var59) * var58;
               }

               if (!entity.getPersistentData().m_128471_("Failed") && var48 % 40.0 == 0.0 && world instanceof ServerLevel _level) {
                  _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_UNLIMITED_VOID.get(), var52, var63, var55, 1, 1.0, 1.0, 1.0, 0.0);
               }

               if (Math.random() < 0.5 && world instanceof ServerLevel _level) {
                  _level.m_8767_(ParticleTypes.f_123810_, var52, var63, var55, 1, 1.0, 1.0, 1.0, 0.0);
               }
            }

            rad += Math.toRadians(10.0 * Math.random());
         }

         double var50 = Math.toRadians(90.0 * (var48 % 4.0));
         double var47 = 4.0 * var48 % (range * 0.5 - 5.0);

         for (int index1 = 0; index1 < 5; index1++) {
            for (int index2 = 0; index2 < 72; index2++) {
               var50 += Math.toRadians(10.0 * Math.random());
               if (Math.random() < 0.025) {
                  double var53 = var66 + Math.cos(var50) * var47;
                  double var64 = var67 + 0.25;
                  double var56 = var68 + Math.sin(var50) * var47;
                  ParticleGeneratorProcedure.execute(
                     world,
                     0.5,
                     1.0,
                     0.0,
                     1.0 * (1.0 - var47 / (range * 0.5 - 5.0)),
                     var53,
                     var66 + Math.cos(var50) * (var47 + 1.0),
                     var64,
                     var67 + 0.25 + Math.random() * 0.25 * 1.0 * (var47 / (range * 0.5 - 5.0)),
                     var56,
                     var68 + Math.sin(var50) * (var47 + 1.0),
                     "minecraft:end_rod"
                  );
               }
            }

            var47++;
         }
      }
   }
}
