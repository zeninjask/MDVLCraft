package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientTechniqueCooldowns;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/** Server to client: ticks left on each technique that is cooling down (techniques not listed are ready). */
public record TechniqueCooldownsPacket(Map<ResourceLocation, Integer> remaining) {
    public static void encode(TechniqueCooldownsPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.remaining, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
    }

    public static TechniqueCooldownsPacket decode(FriendlyByteBuf buf) {
        return new TechniqueCooldownsPacket(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt));
    }

    public static void handle(TechniqueCooldownsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientTechniqueCooldowns.update(packet.remaining)));
        context.get().setPacketHandled(true);
    }
}
