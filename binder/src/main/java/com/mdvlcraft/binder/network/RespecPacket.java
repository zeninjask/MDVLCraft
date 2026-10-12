package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.skills.Respec;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record RespecPacket(String from, String to) {
    public static void encode(RespecPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.from, 32);
        buf.writeUtf(packet.to, 32);
    }

    public static RespecPacket decode(FriendlyByteBuf buf) {
        return new RespecPacket(buf.readUtf(32), buf.readUtf(32));
    }

    public static void handle(RespecPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                if (packet.to.isEmpty()) {
                    Respec.reset(context.get().getSender(), packet.from);
                } else {
                    Respec.change(context.get().getSender(), packet.from, packet.to);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
