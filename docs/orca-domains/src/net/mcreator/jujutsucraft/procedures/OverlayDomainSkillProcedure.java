package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PlayerVariables;
import net.minecraft.world.entity.Entity;

public class OverlayDomainSkillProcedure {
   public static boolean execute(Entity entity) {
      return entity == null
         ? false
         : ((PlayerVariables)entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).PlayerSelectCurseTechnique
            == 20.0;
   }
}
