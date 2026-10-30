package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BulletDomainHit2Procedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         boolean logic_a = false;
         boolean logic_b = false;
         double x_pos = 0.0;
         double y_pos = 0.0;
         double z_pos = 0.0;
         double range = 0.0;
         double dis = 0.0;
         Entity entity_a = null;
         entity.getPersistentData().m_128347_("cnt_bullet_hit", entity.getPersistentData().m_128459_("cnt_bullet_hit") + 1.0);
         if (entity.getPersistentData().m_128459_("cnt_bullet_hit") > 1.0) {
            entity.getPersistentData().m_128347_("cnt_bullet_hit", -15.0);
            if (entity.getPersistentData().m_128459_("NameRanged_ranged") != 0.0 && LogicOwnerExistProcedure.execute(world, entity)) {
               entity_a = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().m_128461_("OWNER_UUID"));
               if (entity.getPersistentData().m_128459_("NameRanged_ranged") == entity_a.getPersistentData().m_128459_("NameRanged")
                  && !entity_a.getPersistentData().m_128471_("Failed")
                  && entity_a instanceof LivingEntity _livEnt10
                  && _livEnt10.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  AABB var10002;
                  int var10004;
                  label46: {
                     Vec3 _center = new Vec3(
                        entity_a.getPersistentData().m_128459_("x_pos_doma"),
                        entity_a.getPersistentData().m_128459_("y_pos_doma"),
                        entity_a.getPersistentData().m_128459_("z_pos_doma")
                     );
                     var10002 = new AABB(_center, _center);
                     if (entity_a instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                        var10004 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
                        break label46;
                     }

                     var10004 = 0;
                  }

                  for (Entity entityiterator : world.m_6443_(
                     Entity.class, var10002.m_82400_(MapVariables.get(world).DomainExpansionRadius * (double)(var10004 > 0 ? 6 : 2) / 2.0), e -> true
                  )) {
                     if (entity != entityiterator
                        && LogicAttackProcedure.execute(world, entity, entityiterator)
                        && LogicAttackDomainProcedure.execute(world, entity, entityiterator)) {
                        x_pos = entityiterator.m_20185_() + (double)entityiterator.m_20205_() * Mth.m_216263_(RandomSource.m_216327_(), -0.5, 0.5);
                        y_pos = entityiterator.m_20186_() + (double)entityiterator.m_20206_() * Mth.m_216263_(RandomSource.m_216327_(), 0.0, 1.0);
                        z_pos = entityiterator.m_20189_() + (double)entityiterator.m_20205_() * Mth.m_216263_(RandomSource.m_216327_(), -0.5, 0.5);
                        entity.m_6021_(x_pos - entity.m_20184_().m_7096_(), y_pos - entity.m_20184_().m_7098_(), z_pos - entity.m_20184_().m_7094_());
                        if (entity instanceof ServerPlayer _serverPlayer) {
                           _serverPlayer.f_8906_
                              .m_9774_(
                                 x_pos - entity.m_20184_().m_7096_(),
                                 y_pos - entity.m_20184_().m_7098_(),
                                 z_pos - entity.m_20184_().m_7094_(),
                                 entity.m_146908_(),
                                 entity.m_146909_()
                              );
                        }

                        entity.m_7618_(Anchor.EYES, new Vec3(x_pos, y_pos, z_pos));
                        break;
                     }
                  }
               }
            }
         }
      }
   }
}
