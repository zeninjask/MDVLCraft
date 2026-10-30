package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NanamiDomainExpansionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range = 0.0;
         double dis = 0.0;
         range = MapVariables.get(world).DomainExpansionRadius * 2.0;
         if (world instanceof ServerLevel _level) {
            _level.m_8767_(
               ParticleTypes.f_123809_,
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               2,
               dis * 0.25,
               dis * 0.25,
               dis * 0.25,
               0.0
            );
         }

         int var10000;
         label56: {
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label56;
            }

            var10000 = 0;
         }

         if (var10000 % 5 == 0) {
            Vec3 _center = new Vec3(
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma")
            );

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(range / 2.0), e -> true)) {
               if (entity != entityiterator && entityiterator instanceof LivingEntity && LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                  if (entityiterator instanceof LivingEntity) {
                     LivingEntity _entity = (LivingEntity)entityiterator;
                     if (!_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.SPECIAL.get(), 10, 0, false, false));
                     }
                  }

                  if (!entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
                     entity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entity.m_20182_(),
                              entity.m_20155_(),
                              entity.m_9236_() instanceof ServerLevel ? (ServerLevel)entity.m_9236_() : null,
                              4,
                              entity.m_7755_().getString(),
                              entity.m_5446_(),
                              entity.m_9236_().m_7654_(),
                              entity
                           ),
                           "particle jujutsucraft:particle_nanami_1 "
                              + entityiterator.m_20185_()
                              + " "
                              + (entityiterator.m_20186_() + (double)entityiterator.m_20206_() * 0.5)
                              + " "
                              + entityiterator.m_20189_()
                              + " "
                              + (double)entityiterator.m_20205_() * 0.5
                              + " "
                              + (double)entityiterator.m_20206_() * 0.5
                              + " "
                              + (double)entityiterator.m_20205_() * 0.5
                              + " 0 "
                              + Math.round(Math.sqrt((double)(entityiterator.m_20205_() + entityiterator.m_20206_())))
                              + " force @s"
                        );
                  }
               }
            }

            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance((MobEffect)JujutsucraftModMobEffects.SPECIAL.get(), 10, 1, false, false));
            }
         }
      }
   }
}
