package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.music.MusicDirector;
import com.mdvlcraft.binder.music.Jingle;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record JinglePacket(Jingle jingle) {
    public static void encode(JinglePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.jingle);
    }

    public static JinglePacket decode(FriendlyByteBuf buf) {
        return new JinglePacket(buf.readEnum(Jingle.class));
    }

    public static void handle(JinglePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MusicDirector.playJingle(packet.jingle)));
        context.get().setPacketHandled(true);
    }
}
