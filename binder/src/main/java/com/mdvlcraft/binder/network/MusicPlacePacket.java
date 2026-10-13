package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.music.MusicDirector;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Whether the player stands inside a village / any structure piece (structures are only known to the server). */
public record MusicPlacePacket(boolean village, boolean structure) {
    public static void encode(MusicPlacePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.village);
        buf.writeBoolean(packet.structure);
    }

    public static MusicPlacePacket decode(FriendlyByteBuf buf) {
        return new MusicPlacePacket(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(MusicPlacePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MusicDirector.setPlace(packet.village, packet.structure)));
        context.get().setPacketHandled(true);
    }
}
