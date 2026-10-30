package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class GetDomainBlockProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         double domain_num;
         double var10000;
         label181: {
            boolean noBarrier = false;
            String outside = "";
            String inside = "";
            String floor = "";
            domain_num = 0.0;
            double close_type = 0.0;
            domain_num = entity.getPersistentData().m_128459_("select") > 0.0
               ? entity.getPersistentData().m_128459_("select")
               : entity.getPersistentData().m_128459_("skill_domain");
            if (!(entity instanceof LivingEntity _livEnt3) || !_livEnt3.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = entity.getPersistentData().m_128459_("cnt2");
               break label181;
            }

            if (entity instanceof LivingEntity _livEnt && _livEnt.m_21023_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
               var10000 = (double)_livEnt.m_21124_((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).m_19564_();
               break label181;
            }

            var10000 = 0.0;
         }

         double var15 = var10000;
         String var11 = "jujutsucraft:jujutsu_barrier";
         String var12 = "jujutsucraft:block_universe";
         String var13 = "jujutsucraft:block_universe";
         if (domain_num <= 10.0) {
            if (domain_num == 1.0) {
               var12 = "jujutsucraft:domain_bone";
               var13 = "jujutsucraft:block_red";
            } else if (domain_num == 2.0) {
               var12 = "jujutsucraft:block_universe";
               var13 = "jujutsucraft:block_universe";
            } else if (domain_num == 3.0) {
               var12 = "jujutsucraft:domain_blue_sky";
            } else if (domain_num == 4.0) {
               var12 = "jujutsucraft:coffinofthe_ironmountain_1";
               var13 = "jujutsucraft:coffinofthe_ironmountain_2";
            } else if (domain_num == 5.0) {
               var13 = "jujutsucraft:block_gravel";
            } else if (domain_num == 6.0) {
               var12 = "jujutsucraft:block_universe";
               var13 = "jujutsucraft:block_universe";
            } else if (domain_num == 7.0) {
               var12 = "jujutsucraft:domain_cloud";
               var13 = "jujutsucraft:domain_cloud";
            } else if (domain_num == 8.0) {
               var12 = "jujutsucraft:domain_blue_sky";
               var13 = "jujutsucraft:domain_sand";
            } else if (domain_num == 9.0) {
               var13 = "jujutsucraft:domain_sand";
            } else if (domain_num == 10.0) {
               var12 = "jujutsucraft:domain_blood";
               var13 = "jujutsucraft:domain_blood";
            }
         } else if (domain_num <= 20.0) {
            if (domain_num == 11.0) {
               var12 = "jujutsucraft:domain_cloud";
               var13 = "jujutsucraft:domain_podzol";
            } else if (domain_num == 12.0) {
               var12 = "jujutsucraft:block_universe";
               var13 = "jujutsucraft:domain_sand_black";
            } else if (domain_num == 13.0) {
               var13 = "jujutsucraft:domain_stone_bricks";
            } else if (domain_num == 14.0) {
               var12 = "jujutsucraft:domain_blue_sky";
               var13 = "jujutsucraft:domain_flower";
            } else if (domain_num == 15.0) {
               var12 = "jujutsucraft:domain_bone";
            } else if (domain_num == 16.0) {
               var12 = "jujutsucraft:block_universe";
            } else if (domain_num == 18.0) {
               var12 = "jujutsucraft:domain_blood";
               var13 = "jujutsucraft:block_red";
            } else if (domain_num == 20.0) {
               var12 = "jujutsucraft:domain_flower";
               var13 = "jujutsucraft:domain_blue_sky";
            }
         } else if (domain_num <= 30.0) {
            if (domain_num == 21.0) {
               var12 = "jujutsucraft:domain_cloud";
               var13 = "jujutsucraft:domain_podzol";
            } else if (domain_num == 22.0) {
               var13 = "jujutsucraft:block_universe";
            } else if (domain_num == 23.0) {
               var12 = "jujutsucraft:domain_bone";
               var13 = "jujutsucraft:block_red";
            } else if (domain_num == 24.0) {
               var12 = "jujutsucraft:domain_ice";
               var13 = "jujutsucraft:domain_ice";
            } else if (domain_num == 25.0) {
               var13 = "jujutsucraft:block_gravel";
            } else if (domain_num == 26.0) {
               var13 = "jujutsucraft:block_gravel";
            } else if (domain_num == 27.0) {
               var13 = "jujutsucraft:domain_planks";
            } else if (domain_num == 28.0) {
               var12 = "jujutsucraft:domain_blue_sky";
               var13 = "jujutsucraft:domain_cloud";
            } else if (domain_num == 29.0) {
               var12 = "jujutsucraft:domain_white";
               var13 = "jujutsucraft:domain_white";
            }
         } else if (domain_num <= 40.0) {
            if (domain_num == 32.0) {
               var12 = "jujutsucraft:domain_sand";
               var13 = "jujutsucraft:domain_sand";
            } else if (domain_num == 33.0) {
               var12 = "jujutsucraft:block_red";
               var13 = "jujutsucraft:domain_planks";
            } else if (domain_num == 34.0) {
               var13 = "jujutsucraft:domain_podzol";
            } else if (domain_num == 35.0) {
               var12 = "jujutsucraft:domain_blood";
               var13 = "jujutsucraft:domain_sand";
            } else if (domain_num == 36.0) {
               var13 = "jujutsucraft:domain_cloud";
            } else if (domain_num == 37.0) {
               var13 = "jujutsucraft:block_red";
            } else if (domain_num == 38.0) {
               var12 = "jujutsucraft:block_universe";
               var13 = "jujutsucraft:block_universe";
            } else if (domain_num == 39.0) {
               var12 = "jujutsucraft:domain_white";
               var13 = "jujutsucraft:domain_white";
            } else if (domain_num == 40.0) {
               var13 = "jujutsucraft:domain_dark_stone";
            }
         } else if (domain_num <= 50.0) {
            if (domain_num == 43.0) {
               var13 = "jujutsucraft:domain_dark_stone";
               var12 = "jujutsucraft:domain_dark_stone";
            } else if (domain_num == 44.0) {
               var13 = "jujutsucraft:domain_white";
               var12 = "jujutsucraft:domain_white";
            } else if (domain_num == 45.0) {
               var13 = "jujutsucraft:block_red";
               var12 = "jujutsucraft:domain_blood";
            } else if (domain_num == 46.0) {
               var12 = "jujutsucraft:domain_blood";
               var13 = "jujutsucraft:domain_blood";
            } else if (domain_num == 47.0) {
               var12 = "jujutsucraft:domain_cloud";
               var13 = "jujutsucraft:coffinofthe_ironmountain_1";
            }
         }

         if (var15 > 0.0) {
            if (!entity.getPersistentData().m_128471_("Failed")) {
               var11 = "minecraft:air";
               var12 = "minecraft:air";
               var13 = "minecraft:air";
            } else {
               var11 = "";
               var12 = "";
            }
         } else if (var15 < 0.0 && !entity.getPersistentData().m_128471_("Failed") && !entity.getPersistentData().m_128471_("Cover")) {
            var11 = "";
            var12 = "";
         }

         entity.getPersistentData().m_128359_("domain_outside", var11);
         entity.getPersistentData().m_128359_("domain_inside", var12);
         entity.getPersistentData().m_128359_("domain_floor", var13);
      }
   }
}
