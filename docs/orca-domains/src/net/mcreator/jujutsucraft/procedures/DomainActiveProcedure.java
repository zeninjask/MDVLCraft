package net.mcreator.jujutsucraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class DomainActiveProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double skill_domain = 0.0;
         skill_domain = entity.getPersistentData().m_128459_("skill_domain");
         if (skill_domain <= 10.0) {
            if (skill_domain == 1.0) {
               MalevolentShrineActiveProcedure.execute(world, entity);
            } else if (skill_domain == 2.0) {
               UnlimitedVoidActiveProcedure.execute(world, entity);
            } else if (skill_domain == 4.0) {
               CoffinoftheIronMountainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 5.0) {
               AuthenticMutualLoveActiveProcedure.execute(world, x, y, z, entity);
            } else if (skill_domain == 6.0) {
               ChimeraShadowGardenActiveProcedure.execute(world, x, y, z, entity);
            } else if (skill_domain == 7.0) {
               KashimoDomainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 8.0) {
               HorizonOfTheCaptivatingSkandhaActiveProcedure.execute(world, entity);
            } else if (skill_domain == 9.0) {
               TsukumoDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 10.0) {
               ChosoDomainActiveProcedure.execute(world, entity);
            }
         } else if (skill_domain <= 20.0) {
            if (skill_domain == 11.0) {
               MeimeiDomainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 12.0) {
               IshigoriDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 13.0) {
               NanamiDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 14.0) {
               CeremonialSeaofLightActiveProcedure.execute(world, x, y, z, entity);
            } else if (skill_domain == 15.0) {
               SelfEmbodimentOfPerfectionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 18.0) {
               WombProfusionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 19.0) {
               TimeCellMoonPalaceActiveProcedure.execute(world, entity);
            } else if (skill_domain == 20.0) {
               DomainExpansionTodoActiveProcedure.execute(world, entity);
            }
         } else if (skill_domain <= 30.0) {
            if (skill_domain == 21.0) {
               ItadoriDomainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 23.0) {
               KurourushiDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 24.0) {
               UraumeDomainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 25.0) {
               GraveyardDomainActiveProcedure.execute(world, entity);
            } else if (skill_domain == 26.0) {
               OgiDomainActiveProcedure.execute(world, x, y, z, entity);
            } else if (skill_domain == 27.0) {
               DeadlySentencingActiveProcedure.execute(world, x, y, z, entity);
            } else if (skill_domain == 29.0) {
               IdleDeathGambleActiveProcedure.execute(world, entity);
            }
         } else if (skill_domain <= 40.0) {
            if (skill_domain == 35.0) {
               JunpeiDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 36.0) {
               NishimiyaDomainExpansionActiveProcedure.execute(world, entity);
            } else if (skill_domain == 40.0) {
               TakumaInoDomainExpansionActiveProcedure.execute(world, entity);
            }
         } else if (skill_domain <= 50.0 && skill_domain == 43.0) {
            RozetsuDomainExpansionActiveProcedure.execute(world, entity);
         }
      }
   }
}
