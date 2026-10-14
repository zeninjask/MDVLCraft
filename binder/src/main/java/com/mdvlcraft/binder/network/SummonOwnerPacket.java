package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientSummonOwners;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Who summoned an Iron's summon (Iron's keeps it on the server only), sent when a player starts tracking the summon. */
public record SummonOwnerPacket(int summon, UUID owner) {
    public static void encode(SummonOwnerPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.summon);
        buf.writeUUID(packet.owner);
    }

    public static SummonOwnerPacket decode(FriendlyByteBuf buf) {
        return new SummonOwnerPacket(buf.readVarInt(), buf.readUUID());
    }

    public static void handle(SummonOwnerPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSummonOwners.set(packet.summon, packet.owner)));
        context.get().setPacketHandled(true);
    }
}
