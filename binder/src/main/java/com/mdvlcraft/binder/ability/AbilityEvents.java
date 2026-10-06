package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class AbilityEvents {
    private AbilityEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // The wheel must reach the client even when no ability is granted.
        AbilityGrants.markDirty((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AbilityGrants.flush(event.getServer());
        }
    }
}
