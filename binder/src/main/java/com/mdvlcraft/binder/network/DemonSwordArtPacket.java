package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.DemonSwordArts;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** Both mouse buttons were pressed while holding a demon sword. */
public record DemonSwordArtPacket() {
    public static void encode(DemonSwordArtPacket packet, FriendlyByteBuf buf) {
    }

    public static DemonSwordArtPacket decode(FriendlyByteBuf buf) {
        return new DemonSwordArtPacket();
    }

    public static void handle(DemonSwordArtPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                DemonSwordArts.cast(context.get().getSender());
            }
        });
        context.get().setPacketHandled(true);
    }
}
