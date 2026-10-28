package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.FriendlySummonMarker;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** The friendly summons near the player, by entity id: true for their own, false for an ally's (modified/FriendlySummons). */
public record FriendlySummonsPacket(Map<Integer, Boolean> summons) {
    public static void encode(FriendlySummonsPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.summons, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeBoolean);
    }

    public static FriendlySummonsPacket decode(FriendlyByteBuf buf) {
        return new FriendlySummonsPacket(buf.readMap(HashMap::new, FriendlyByteBuf::readVarInt, FriendlyByteBuf::readBoolean));
    }

    public static void handle(FriendlySummonsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FriendlySummonMarker.set(packet.summons)));
        context.get().setPacketHandled(true);
    }
}
