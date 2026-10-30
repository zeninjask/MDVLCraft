package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.PartyPositions;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

/** Where the player's party members in the same dimension are (content/PartyPositions), for the far trackers. */
public record PartyPositionsPacket(List<UUID> ids, List<Vec3> positions) {
    public static void encode(PartyPositionsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.ids.size());
        for (int i = 0; i < packet.ids.size(); i++) {
            buf.writeUUID(packet.ids.get(i));
            Vec3 pos = packet.positions.get(i);
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
        }
    }

    public static PartyPositionsPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<UUID> ids = new ArrayList<>();
        List<Vec3> positions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUUID());
            positions.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new PartyPositionsPacket(ids, positions);
    }

    public static void handle(PartyPositionsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> PartyPositions.setClient(packet.ids, packet.positions));
        context.get().setPacketHandled(true);
    }
}
