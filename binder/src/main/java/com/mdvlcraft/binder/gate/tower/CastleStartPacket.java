package com.mdvlcraft.binder.gate.tower;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Client to server: the floor picked in the Castle Sigil's start menu, for the gate at `base` (CastleSigilItem.menuChoice). */
public record CastleStartPacket(BlockPos base, int floor) {
    public static void encode(CastleStartPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.base);
        buf.writeVarInt(packet.floor);
    }

    public static CastleStartPacket decode(FriendlyByteBuf buf) {
        return new CastleStartPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(CastleStartPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                CastleSigilItem.menuChoice(player, packet.base, packet.floor);
            }
        });
        context.get().setPacketHandled(true);
    }
}
