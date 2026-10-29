package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.LanternBelt;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player pressed Activate Belt. */
public record ActivateBeltPacket() {
    public static void encode(ActivateBeltPacket packet, FriendlyByteBuf buf) {
    }

    public static ActivateBeltPacket decode(FriendlyByteBuf buf) {
        return new ActivateBeltPacket();
    }

    public static void handle(ActivateBeltPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                LanternBelt.activate(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
