package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.VehicleBlinkClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Moves the rider's own vehicle (the Void Strider's blink): the rider's client owns where a ridden mount is. */
public record VehicleBlinkPacket(int vehicle, double x, double y, double z) {
    public static void encode(VehicleBlinkPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.vehicle);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
    }

    public static VehicleBlinkPacket decode(FriendlyByteBuf buf) {
        return new VehicleBlinkPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(VehicleBlinkPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VehicleBlinkClient.blink(packet)));
        context.get().setPacketHandled(true);
    }
}
