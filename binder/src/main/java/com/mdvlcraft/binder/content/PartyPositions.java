package com.mdvlcraft.binder.content;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.PartyPositionsPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Twice a second each player is sent where their party members in the same dimension are, so the client can draw
 * Parties' tracker for members too far away to be loaded (client/PartyFarTrackers, while the player list key is held).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PartyPositions {
    private static final int INTERVAL = 10;
    /** On the client: the last positions sent. */
    private static Map<UUID, Vec3> client = Map.of();

    private PartyPositions() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % INTERVAL != 0) {
            return;
        }
        List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            List<UUID> ids = new ArrayList<>();
            List<Vec3> positions = new ArrayList<>();
            for (ServerPlayer other : players) {
                if (other != player && other.level() == player.level() && AllianceAPI.areAllied(player, other)) {
                    ids.add(other.getUUID());
                    positions.add(other.position());
                }
            }
            if (!ids.isEmpty() || !client.isEmpty()) {
                BinderNetwork.sendTo(player, new PartyPositionsPacket(ids, positions));
            }
        }
    }

    /** PartyPositionsPacket, on the client. */
    public static void setClient(List<UUID> ids, List<Vec3> positions) {
        Map<UUID, Vec3> map = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            map.put(ids.get(i), positions.get(i));
        }
        client = map;
    }

    public static Map<UUID, Vec3> client() {
        return client;
    }
}
