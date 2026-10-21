package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.RaijuClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** A player entered (`on`) or left Yakusanoikazuchi's form (raiju/RaijuForm). */
public record RaijuFormPacket(int entity, boolean on) {
    public static void encode(RaijuFormPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeBoolean(packet.on);
    }

    public static RaijuFormPacket decode(FriendlyByteBuf buf) {
        return new RaijuFormPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(RaijuFormPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RaijuClient.handle(packet)));
        context.get().setPacketHandled(true);
    }
}
