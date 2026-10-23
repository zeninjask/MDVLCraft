package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientPhantomRush;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Assassination Rush state: the caster's picked targets (out of {@code total}) and the target it is assassinating (-1 none). */
public record PhantomRushPacket(int player, int fastTarget, List<UUID> picked, int total) {
    public static void encode(PhantomRushPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.player);
        buf.writeVarInt(packet.fastTarget);
        buf.writeCollection(packet.picked, FriendlyByteBuf::writeUUID);
        buf.writeVarInt(packet.total);
    }

    public static PhantomRushPacket decode(FriendlyByteBuf buf) {
        return new PhantomRushPacket(buf.readVarInt(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readUUID), buf.readVarInt());
    }

    public static void handle(PhantomRushPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhantomRush.update(packet)));
        context.get().setPacketHandled(true);
    }
}
