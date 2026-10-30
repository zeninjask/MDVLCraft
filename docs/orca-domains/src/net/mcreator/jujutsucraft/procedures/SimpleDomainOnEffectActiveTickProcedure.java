package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SimpleDomainOnEffectActiveTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         int var10000;
         label128: {
            boolean logic_infinity = false;
            double z_pos = 0.0;
            double num3 = 0.0;
            double strength = 0.0;
            double num1 = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double pitch = 0.0;
            double yaw = 0.0;
            double num2 = 0.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19564_();
               break label128;
            }

            var10000 = 0;
         }

         if (var10000 > 0 && LogicSimpleDomainProcedure.execute()) {
            label120: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19557_();
                  break label120;
               }

               var10000 = 0;
            }

            if (var10000 % 5 == 1) {
               SimpleDomainEffectStartedappliedProcedure.execute(world, x, y, z, entity);
            }

            if (!ActiveTickConditionProcedure.execute(entity)) {
               if (entity instanceof LivingEntity _entity) {
                  _entity.m_21195_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
               }

               return;
            }

            label112: {
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19557_();
                  break label112;
               }

               var10000 = 0;
            }

            if (var10000 % 2 == 1) {
               double var34 = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique;
               double var39 = ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerCurseTechnique2;
               if (entity.m_6095_().m_204039_(TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("jujutsucraft:can_use_hollow_wicker_basket")))
                  || entity instanceof LivingEntity _livEnt5 && _livEnt5.m_21023_((MobEffect)JujutsucraftModMobEffects.SUKUNA_EFFECT.get())
                  || entity instanceof Player
                     && (
                        var34 == 1.0
                           || var39 == 1.0
                           || var34 == 7.0
                           || var39 == 7.0
                           || var34 == 12.0
                           || var39 == 12.0
                           || var34 == 24.0
                           || var39 == 24.0
                           || var34 == 44.0
                           || var39 == 44.0
                           || var34 == 45.0
                           || var39 == 45.0
                     )) {
                  double var38 = Math.toRadians((double)(entity.m_146908_() + 90.0F));
                  double var37 = Math.toRadians((double)entity.m_146909_());
                  double var35 = entity.m_20185_() + Math.cos(var38) * Math.cos(var37) * (double)(-1.0F - entity.m_20205_());
                  double var36 = entity.m_20186_() + (double)entity.m_20206_() * 0.5;
                  double var33 = entity.m_20189_() + Math.sin(var38) * Math.cos(var37) * (double)(-1.0F - entity.m_20205_());
                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(
                        (SimpleParticleType)JujutsucraftModParticleTypes.PARTICLE_HOLLOW_WICKER_BASKET.get(), var35, var36, var33, 0, 0.0, 0.0, 0.0, 0.0
                     );
                  }
               }
            }

            int var10002;
            label83: {
               var50 = (double)entity.m_20205_();
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                  var10002 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19557_();
                  break label83;
               }

               var10002 = 0;
            }

            double var40 = var50 + 0.025 * (double)var10002;
            var40 = Math.min(var40, entity.getPersistentData().m_128459_("skill") == 3105.0 ? 16.0 : 4.0);
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(var40 * 2.0 / 2.0), e -> true)) {
               if (entity != entityiterator
                  && entity.m_20186_() + (double)entity.m_20206_() >= entityiterator.m_20186_()
                  && entity.m_20186_() <= entityiterator.m_20186_() + (double)entityiterator.m_20206_()) {
                  if (entityiterator instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), 5, 0, true, true));
                  }
                  break;
               }
            }
         }
      }
   }
}
