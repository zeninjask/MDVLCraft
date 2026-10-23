package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.network.BinderNetwork;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * The Demon Castle leaderboard line on the status page (plan 10.4): the player's best floor and the server's best floor
 * with its holder's name. Sent on login and whenever tower progress changes (TowerProgress.saveNow); the client keeps
 * the last one in the static fields below, which TowerClient.statusLine reads.
 */
public record TowerStatusPacket(int best, int topFloor, String topName) {
    public static volatile int clientBest;
    public static volatile int clientTopFloor;
    public static volatile String clientTopName = "";

    public static void encode(TowerStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.best);
        buf.writeVarInt(packet.topFloor);
        buf.writeUtf(packet.topName, 64);
    }

    public static TowerStatusPacket decode(FriendlyByteBuf buf) {
        return new TowerStatusPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(TowerStatusPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            clientBest = packet.best;
            clientTopFloor = packet.topFloor;
            clientTopName = packet.topName;
        });
        context.get().setPacketHandled(true);
    }

    public static void send(ServerPlayer player) {
        BinderNetwork.sendTo(player, of(player, top(player.server)));
    }

    public static void sendAll(MinecraftServer server) {
        Map.Entry<UUID, TowerProgress.Entry> top = top(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BinderNetwork.sendTo(player, of(player, top));
        }
    }

    private static TowerStatusPacket of(ServerPlayer player, Map.Entry<UUID, TowerProgress.Entry> top) {
        int best = TowerProgress.get(player.server).of(player.getUUID()).best;
        if (top == null) {
            return new TowerStatusPacket(best, 0, "");
        }
        String name = player.server.getProfileCache() == null ? null
            : player.server.getProfileCache().get(top.getKey()).map(profile -> profile.getName()).orElse(null);
        return new TowerStatusPacket(best, top.getValue().best, name != null ? name : "?");
    }

    private static Map.Entry<UUID, TowerProgress.Entry> top(MinecraftServer server) {
        List<Map.Entry<UUID, TowerProgress.Entry>> top = TowerProgress.get(server).top();
        return top.isEmpty() || top.get(0).getValue().best <= 0 ? null : top.get(0);
    }
}
