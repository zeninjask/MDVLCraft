package com.mdvlcraft.binder.gate.tower;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Server to client: open the Castle Sigil's start menu (plan 10.3) for a gate at `base`, with the user's reached floor and
 * checkpoint. The choice comes back as a CastleStartPacket.
 */
public record CastleMenuPacket(BlockPos base, int reached, int checkpoint) {
    public static void encode(CastleMenuPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.base);
        buf.writeVarInt(packet.reached);
        buf.writeVarInt(packet.checkpoint);
    }

    public static CastleMenuPacket decode(FriendlyByteBuf buf) {
        return new CastleMenuPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(CastleMenuPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.mdvlcraft.binder.gate.tower.client.CastleStartScreen.open(packet.base, packet.reached, packet.checkpoint)));
        context.get().setPacketHandled(true);
    }
}
