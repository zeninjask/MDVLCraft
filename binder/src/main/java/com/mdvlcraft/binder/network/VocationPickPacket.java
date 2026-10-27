package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.skills.VocationPick;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The vocation picker's choice: the vocation's two archetypes (skills/VocationPick.pick). */
public record VocationPickPacket(String first, String second) {
    public static void encode(VocationPickPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.first, 32);
        buf.writeUtf(packet.second, 32);
    }

    public static VocationPickPacket decode(FriendlyByteBuf buf) {
        return new VocationPickPacket(buf.readUtf(32), buf.readUtf(32));
    }

    public static void handle(VocationPickPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                VocationPick.pick(context.get().getSender(), packet.first, packet.second);
            }
        });
        context.get().setPacketHandled(true);
    }
}
