package net.mcreator.jujutsucraft.procedures;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class DomainExpansionEntityOnInitialEntitySpawnProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
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
                  "data merge entity @s {NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,NoAI:1b}"
               );
         }

         entity.getPersistentData().m_128347_("x_pos", entity.m_20185_());
         entity.getPersistentData().m_128347_("y_pos", entity.m_20186_());
         entity.getPersistentData().m_128347_("z_pos", entity.m_20189_());
      }
   }
}
