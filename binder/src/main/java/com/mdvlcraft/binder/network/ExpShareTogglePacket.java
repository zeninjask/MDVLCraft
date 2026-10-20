package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.ExpShare;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The Social Interactions screen opened (toggle false: send the current setting) or its Experience Share button was pressed (content/ExpShare). */
public record ExpShareTogglePacket(boolean toggle) {
    public static void encode(ExpShareTogglePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.toggle);
    }

    public static ExpShareTogglePacket decode(FriendlyByteBuf buf) {
        return new ExpShareTogglePacket(buf.readBoolean());
    }

    public static void handle(ExpShareTogglePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (packet.toggle) {
                ExpShare.toggle(context.get().getSender());
            } else {
                ExpShare.sync(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
