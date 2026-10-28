package com.mdvlcraft.binder.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "mdvlcraft")
public final class AbilityEvents {
    private AbilityEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        AbilityGrants.markDirty((ServerPlayer)event.getEntity());
        BlackBirdManipulation.deactivate((ServerPlayer)event.getEntity());
        // Cursed Fate's flow aura is off in the pack (mixin/cursedfate/FlowAuraOnTickProcedureMixin): anyone left in it is let out
        ServerPlayer player = (ServerPlayer)event.getEntity();
        cursedfate.network.CursedfateModVariables.PlayerVariables variables = TechniqueRunner.variables(player);
        if (variables.Flow || variables.FlowAuraActivated) {
            variables.Flow = false;
            variables.FlowAuraActivated = false;
            variables.syncPlayerVariables(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            AbilityGrants.flush(event.getServer());
            TechniqueRunner.tick(event.getServer());
            PhantomRush.tick(event.getServer());
            DomainUpkeep.tick(event.getServer());
            SilentAppraisal.tick(event.getServer());
        }
    }
}
