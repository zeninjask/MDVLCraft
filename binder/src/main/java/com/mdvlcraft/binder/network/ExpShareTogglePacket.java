package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.ExpShare;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The Social Interactions screen's Experience Share button was pressed (content/ExpShare). */
public record ExpShareTogglePacket() {
    public static void encode(ExpShareTogglePacket packet, FriendlyByteBuf buf) {
    }

    public static ExpShareTogglePacket decode(FriendlyByteBuf buf) {
        return new ExpShareTogglePacket();
    }

    public static void handle(ExpShareTogglePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> ExpShare.toggle(context.get().getSender()));
        context.get().setPacketHandled(true);
    }
}
