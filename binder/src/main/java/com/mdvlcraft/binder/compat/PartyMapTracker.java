package com.mdvlcraft.binder.compat;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import xaero.common.server.MinecraftServerData;
import xaero.common.server.radar.tracker.ISyncedPlayerTrackerSystem;

/**
 * Party members (Parties) show on each other's Xaero's minimap and world map wherever they are: Xaero's Minimap on the
 * server sends a player the positions of everyone this system tracks for them, the way it does for FTB Teams.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PartyMapTracker implements ISyncedPlayerTrackerSystem {
    private PartyMapTracker() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServerData.get(event.getServer()).getSyncedPlayerTrackerSystemManager().register("mdvlcraft_parties", new PartyMapTracker());
    }

    /** 2 is Xaero's level for teammates (FTB Teams gives allies 1). */
    @Override
    public int getTrackingLevel(Player tracker, Player tracked) {
        return AllianceAPI.areAllied(tracker, tracked) ? 2 : 0;
    }

    @Override
    public boolean isPartySystem() {
        return true;
    }
}
