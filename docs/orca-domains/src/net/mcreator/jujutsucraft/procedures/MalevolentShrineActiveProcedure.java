package net.mcreator.jujutsucraft.procedures;

import java.util.ArrayList;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.MapVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class MalevolentShrineActiveProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         double count_A;
         int var10001;
         label186: {
            String STR1 = "";
            double old_skill = 0.0;
            count_A = 0.0;
            double z_center = 0.0;
            double range = 0.0;
            double old_cooldown = 0.0;
            double x_pos = 0.0;
            double z_pos = 0.0;
            double y_center = 0.0;
            double dis = 0.0;
            double dust_amount = 0.0;
            double x_center = 0.0;
            double num1 = 0.0;
            double y_pos = 0.0;
            double num2 = 0.0;
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10001 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
               break label186;
            }

            var10001 = 0;
         }

         double var39 = MapVariables.get(world).DomainExpansionRadius * (double)(var10001 > 0 ? 18 : 2);
         double var36 = entity.getPersistentData().m_128459_("skill");
         double var40 = entity.getPersistentData().m_128459_("COOLDOWN_TICKS");
         entity.getPersistentData().m_128347_("y_knockback", -0.1);

         for (int index0 = 0; index0 < 4; index0++) {
            double var41;
            double var44;
            double var54;
            int var10000;
            label177: {
               double var51 = count_A % 2.0 == 0.0 ? 1.0 : -1.0;
               double var57 = count_A % 4.0 <= 1.0 ? 1.0 : -1.0;
               var41 = entity.getPersistentData().m_128459_("x_pos_doma") + var39 * 0.25 * var51;
               var54 = entity.getPersistentData().m_128459_("y_pos_doma") + var39 * 0.25 - 15.0;
               var44 = entity.getPersistentData().m_128459_("z_pos_doma") + var39 * 0.25 * var57;
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10000 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label177;
               }

               var10000 = 0;
            }

            if (var10000 % 2 == 1) {
               entity.getPersistentData().m_128347_("skill", 105.0);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 50.0);
            } else {
               entity.getPersistentData().m_128347_("skill", 106.0);
               entity.getPersistentData().m_128347_("COOLDOWN_TICKS", 100.0);
            }

            int var10003;
            label171: {
               var73 = entity.getPersistentData();
               if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                  var10003 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                  break label171;
               }

               var10003 = 0;
            }

            var73.m_128347_("Damage", 10.5 + (double)(var10003 % 10) * 0.1);
            entity.getPersistentData().m_128347_("Range", var39 * 0.5);
            entity.getPersistentData().m_128347_("effect", 1.0);
            entity.getPersistentData().m_128347_("effectConfirm", 1.0);
            entity.getPersistentData().m_128347_("knockback", 0.001);
            entity.getPersistentData().m_128379_("swing", false);
            entity.getPersistentData().m_128379_("attack", false);
            entity.getPersistentData().m_128379_("DomainAttack", true);
            entity.getPersistentData().m_128379_("ExtinctionBlock", true);
            RangeAttackProcedure.execute(world, var41, var54, var44, entity);
            count_A++;
         }

         int var74;
         label163: {
            entity.getPersistentData().m_128379_("ExtinctionBlock", false);
            entity.getPersistentData().m_128347_("y_knockback", 0.0);
            entity.getPersistentData().m_128347_("skill", var36);
            entity.getPersistentData().m_128347_("COOLDOWN_TICKS", var40);
            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var74 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
               break label163;
            }

            var74 = 0;
         }

         if (var74 > 0) {
            if (entity.getPersistentData().m_128459_("dust_amount") <= 0.0) {
               entity.getPersistentData().m_128347_("dust_amount", 1.0);
            }

            double var50 = entity.getPersistentData().m_128459_("x_pos_doma");
            double var47 = entity.getPersistentData().m_128459_("y_pos_doma");
            double var38 = entity.getPersistentData().m_128459_("z_pos_doma");
            double var52 = 0.0;

            for (int index1 = 0; index1 < 512; index1++) {
               count_A = Math.toRadians(Math.random() * 360.0);
               double var48 = var39 * 0.5 * (Math.random() * 2.0 - 1.0);
               double var42 = var50 + Math.sin(count_A) * var48;
               double var55 = var47 + Math.random() * var39 * 0.2;
               double var45 = var38 + Math.cos(count_A) * var48;
               if (!world.m_46859_(BlockPos.m_274561_(var42, var55, var45))) {
                  entity.getPersistentData().m_128347_("dust_amount", Math.min(entity.getPersistentData().m_128459_("dust_amount") + 1.0, 200.0));
                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123813_, var42, var55, var45, 2, 1.5, 1.5, 1.5, 0.0);
                  }

                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123796_, var42, var55, var45, 2, 1.5, 1.5, 1.5, 0.5);
                  }

                  entity.getPersistentData().m_128379_("noParticle", true);
                  entity.getPersistentData().m_128379_("ExtinctionBlock", true);
                  entity.getPersistentData().m_128347_("BlockRange", 16.0);
                  entity.getPersistentData().m_128347_("BlockDamage", 99.0);
                  BlockDestroyAllDirectionProcedure.execute(world, var42, Math.max(var55, var47 + 8.0), var45, entity);
                  if (var52 < 32.0 || var52 / 512.0 < Math.random()) {
                     break;
                  }
               }

               var52++;
            }

            double var49 = (double)Math.round(entity.getPersistentData().m_128459_("dust_amount") / 200.0 * 10.0);
            if (entity instanceof Player) {
               label140: {
                  if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                     var74 = _livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19557_();
                     break label140;
                  }

                  var74 = 0;
               }

               if (var74 % 10 == 0) {
                  String _setval = "DUST";
                  entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                     capability.OVERLAY1 = _setval;
                     capability.syncPlayerVariables(entity);
                  });
                  _setval = "";
                  entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                     capability.OVERLAY2 = _setval;
                     capability.syncPlayerVariables(entity);
                  });
                  String var35 = "§l§4";
                  var52 = 0.0;

                  for (int index2 = 0; index2 < 10; index2++) {
                     if (var52 == var49) {
                        var35 = var35 + "§r§7";
                     }

                     var52++;
                     var35 = var35 + "■";
                  }

                  entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                     capability.OVERLAY2 = var35;
                     capability.syncPlayerVariables(entity);
                  });
               }
            }

            double var43 = entity.getPersistentData().m_128459_("x_pos_doma");
            double var56 = entity.getPersistentData().m_128459_("y_pos_doma");
            double var46 = entity.getPersistentData().m_128459_("z_pos_doma");
            if (entity.getPersistentData().m_128459_("skill") == 107.0) {
               for (int index3 = 0; index3 < (int)(var49 + 1.0); index3++) {
                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123790_, var43, var56, var46, 50, var39 * 0.25, var39 * 0.25, var39 * 0.25, 0.1);
                  }
               }
            } else {
               for (int index4 = 0; index4 < (int)(var49 + 1.0); index4++) {
                  if (world instanceof ServerLevel _level) {
                     _level.m_8767_(ParticleTypes.f_123783_, var43, var56, var46, 50, var39 * 0.25, var39 * 0.25, var39 * 0.25, 0.1);
                  }
               }
            }

            if (!entity.getPersistentData().m_128471_("Failed")) {
               for (Entity entityiterator : new ArrayList(world.m_6907_())) {
                  if (CanSeeSukunaSlashProcedure.execute(world, entity, entityiterator)
                     && !entityiterator.m_9236_().m_5776_()
                     && entityiterator.m_20194_() != null) {
                     entityiterator.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              entityiterator.m_20182_(),
                              entityiterator.m_20155_(),
                              entityiterator.m_9236_() instanceof ServerLevel ? (ServerLevel)entityiterator.m_9236_() : null,
                              4,
                              entityiterator.m_7755_().getString(),
                              entityiterator.m_5446_(),
                              entityiterator.m_9236_().m_7654_(),
                              entityiterator
                           ),
                           "particle jujutsucraft:particle_slash_large "
                              + var50
                              + " "
                              + var47
                              + " "
                              + var38
                              + " "
                              + var39 * 0.25
                              + " "
                              + var39 * 0.25
                              + " "
                              + var39 * 0.25
                              + " 0.01 "
                              + Math.round(4.0 * var39)
                              + " normal"
                        );
                  }
               }
            }
         }
      }
   }
}
