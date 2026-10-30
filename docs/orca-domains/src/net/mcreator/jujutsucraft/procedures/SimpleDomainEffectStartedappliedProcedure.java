package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SimpleDomainEffectStartedappliedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         int var10000;
         label46: {
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double num1 = 0.0;
            double num2 = 0.0;
            double num3 = 0.0;
            double pitch = 0.0;
            double yaw = 0.0;
            double tick = 0.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
               var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19564_();
               break label46;
            }

            var10000 = 0;
         }

         if (var10000 > 0 && LogicSimpleDomainProcedure.execute()) {
            int var10002;
            label38: {
               var35 = (double)entity.m_20205_();
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
                  var10002 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).m_19557_();
                  break label38;
               }

               var10002 = 0;
            }

            double var32 = var35 + 0.025 * (double)var10002;
            var32 = Math.min(var32, entity.getPersistentData().m_128459_("skill") == 3105.0 ? 16.0 : 4.0);

            for (int index0 = 0; index0 < 72; index0++) {
               double var31 = Math.toRadians(Math.random() * 360.0);
               double var29 = x + Math.sin(var31) * var32;
               double var30 = z + Math.cos(var31) * var32;
               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(var29, y, var30), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "particle dust 0.749 0.984 1.000 1 ~ ~ ~ 0 0 0 1 1 force"
                     );
               }
            }
         }
      }
   }
}
