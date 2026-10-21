package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.gaunt.GauntLeap;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The client's Haunted Lord touched a block face mid-leap (`face`, a Direction 3D data value), or jumped off it (-1). */
public record GauntClingPacket(int face) {
    public static void encode(GauntClingPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.face);
    }

    public static GauntClingPacket decode(FriendlyByteBuf buf) {
        return new GauntClingPacket(buf.readVarInt());
    }

    public static void handle(GauntClingPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (packet.face >= -1 && packet.face < 6) {
                GauntLeap.cling(context.get().getSender(), packet.face < 0 ? null : Direction.from3DDataValue(packet.face));
            }
        });
        context.get().setPacketHandled(true);
    }
}
