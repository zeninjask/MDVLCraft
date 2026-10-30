package net.mcreator.jujutsucraft.procedures;

import java.util.Comparator;
import net.mcreator.jujutsucraft.entity.DomainExpansionEntityEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BreakDomainProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         int var10000;
         label21: {
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double range = 0.0;
            double dis = 0.0;
            double x_dis = 0.0;
            double y_dis = 0.0;
            double z_dis = 0.0;
            double loop_num = 0.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
               break label21;
            }

            var10000 = 0;
         }

         if (var10000 <= 0) {
            double var21 = entity.getPersistentData().m_128459_("x_pos_doma");
            double var22 = entity.getPersistentData().m_128459_("y_pos_doma");
            double var23 = entity.getPersistentData().m_128459_("z_pos_doma");
            if (!world.m_6443_(DomainExpansionEntityEntity.class, AABB.m_165882_(new Vec3(var21, var22, var23), 0.1, 0.1, 0.1), e -> true).isEmpty()) {
               world.m_6443_(DomainExpansionEntityEntity.class, AABB.m_165882_(new Vec3(var21, var22, var23), 0.1, 0.1, 0.1), e -> true)
                  .stream()
                  .sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(var21, var22, var23))
                  .findFirst()
                  .orElse(null)
                  .getPersistentData()
                  .m_128379_("Break", true);
            }
         }
      }
   }
}
