package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.DemonicAssimilationDismiss;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player right-clicked while sneaking in Demonic Assimilation. */
public record DismissAssimilationPacket() {
    public static void encode(DismissAssimilationPacket packet, FriendlyByteBuf buf) {
    }

    public static DismissAssimilationPacket decode(FriendlyByteBuf buf) {
        return new DismissAssimilationPacket();
    }

    public static void handle(DismissAssimilationPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                DemonicAssimilationDismiss.dismiss(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
