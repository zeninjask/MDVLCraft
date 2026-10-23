package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientArsenal;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player's Formless Sword mode, picked weapon, Shadow Arsenal and sword wheel (shadow/ShadowArsenal). */
public record ArsenalSyncPacket(int mode, String pick, List<String> arsenal, List<String> wheel) {
    public static void encode(ArsenalSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.mode);
        buf.writeUtf(packet.pick);
        buf.writeCollection(packet.arsenal, FriendlyByteBuf::writeUtf);
        buf.writeCollection(packet.wheel, FriendlyByteBuf::writeUtf);
    }

    public static ArsenalSyncPacket decode(FriendlyByteBuf buf) {
        return new ArsenalSyncPacket(buf.readVarInt(), buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf));
    }

    public static void handle(ArsenalSyncPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            ClientArsenal.update(packet.mode, packet.pick, packet.arsenal, packet.wheel)));
        context.get().setPacketHandled(true);
    }
}
