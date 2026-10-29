package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.traits.Traits;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player's traits by slot ("" for an empty slot) and how many slots they have, for the Status page. */
public record SyncTraitsPacket(List<String> traits, int slots) {
    public static void encode(SyncTraitsPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.traits, FriendlyByteBuf::writeUtf);
        buf.writeVarInt(packet.slots);
    }

    public static SyncTraitsPacket decode(FriendlyByteBuf buf) {
        return new SyncTraitsPacket(buf.readList(FriendlyByteBuf::readUtf), buf.readVarInt());
    }

    public static void handle(SyncTraitsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> Traits.setClient(packet.traits, packet.slots));
        context.get().setPacketHandled(true);
    }
}
