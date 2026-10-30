package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
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

public class CeremonialSeaofLightActiveProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double range;
         double var10000;
         label78: {
            double distance = 0.0;
            double ticks = 0.0;
            double num1 = 0.0;
            range = 0.0;
            double rad1 = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label78;
            }

            var10000 = 0.0;
         }

         double var30 = var10000;
         if (var30 % 10.0 == 0.0) {
            entity.getPersistentData().m_128379_("onlyLiving", true);
            Vec3 _center = new Vec3(
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma")
            );

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
               if (entity != entityiterator
                  && LogicAttackDomainProcedure.execute(world, entity, entityiterator)
                  && !entityiterator.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:ranged_ammo")))) {
                  double var31 = 20.0;
                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19613_, (int)var31, 4, false, false));
                     }
                  }

                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, (int)var31, 2, false, false));
                     }
                  }
               }
            }

            entity.getPersistentData().m_128379_("onlyLiving", false);
         }

         if (entity.getPersistentData().m_128471_("Failed")) {
            if (entity.m_20096_() && world instanceof ServerLevel _level) {
               _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_FLOWER.get(), x, y, z, 20, 1.0, 0.0, 1.0, 0.05);
            }
         } else {
            double var33 = Math.toRadians(90.0 * (var30 % 4.0));
            double var29 = 4.0 * var30 % (range * 0.5 - 5.0);

            for (int index0 = 0; index0 < 5; index0++) {
               for (int index1 = 0; index1 < 72; index1++) {
                  if (!(Math.random() < 0.9)) {
                     var33 += Math.toRadians(10.0 * Math.random());
                     double var34 = entity.getPersistentData().m_128459_("x_pos_doma") + Math.cos(var33) * var29;
                     double var35 = entity.getPersistentData().m_128459_("y_pos_doma") + 0.1;
                     double var36 = entity.getPersistentData().m_128459_("z_pos_doma") + Math.sin(var33) * var29;
                     if (world instanceof ServerLevel _level) {
                        _level.m_8767_((SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_FLOWER.get(), var34, var35, var36, 1, 0.2, 0.1, 0.2, 0.01);
                     }
                  }
               }

               var29++;
            }
         }
      }
   }
}
