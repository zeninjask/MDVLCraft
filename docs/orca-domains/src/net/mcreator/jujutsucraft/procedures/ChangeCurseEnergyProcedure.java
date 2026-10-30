package net.mcreator.jujutsucraft.procedures;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ChangeCurseEnergyProcedure {
   public static void execute(Entity entity, double energy_change) {
      if (entity != null) {
         if (entity instanceof Player && energy_change != 0.0) {
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
               .ifPresent(capability -> capability.PlayerCursePowerChange += energy_change);
         }
      }
   }
}
