package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class SelfEmbodimentOfPerfectionActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double range;
         int var10000;
         label23: {
            double old_skill = 0.0;
            range = 0.0;
            double old_cooldown = 0.0;
            range = MapVariables.get(world).DomainExpansionRadius * 2.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
               break label23;
            }

            var10000 = 0;
         }

         if (var10000 % 40 == 20 || entity.getPersistentData().m_128471_("StartDomainAttack")) {
            entity.getPersistentData().m_128379_("StartDomainAttack", false);
            double var9 = entity.getPersistentData().m_128459_("skill");
            double var11 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 50.0);
            entity.getPersistentData().m_128347_("skill", 1505.0);
            entity.getPersistentData().m_128347_("Damage", 24.0);
            entity.getPersistentData().m_128347_("Range", range);
            entity.getPersistentData().m_128347_("effect", 12.0);
            entity.getPersistentData().m_128347_("effectConfirm", 2.0);
            entity.getPersistentData().m_128347_("knockback", 0.0);
            entity.getPersistentData().m_128379_("swing", false);
            entity.getPersistentData().m_128379_("attack", false);
            entity.getPersistentData().m_128379_("onlyLiving", true);
            entity.getPersistentData().m_128379_("DomainAttack", true);
            RangeAttackProcedure.execute(
               world,
               entity.getPersistentData().m_128459_("x_pos_doma"),
               entity.getPersistentData().m_128459_("y_pos_doma"),
               entity.getPersistentData().m_128459_("z_pos_doma"),
               entity
            );
            entity.getPersistentData().m_128347_("skill", var9);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var11);
            entity.getPersistentData().m_128379_("onlyLiving", false);
         }
      }
   }
}
