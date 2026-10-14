package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientArchetypes;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** A player's picked archetypes, highest level first, and that highest level (for the party windows). */
public record ArchetypeLevelsPacket(UUID player, List<String> archetypes, int level) {
    public static void encode(ArchetypeLevelsPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeCollection(packet.archetypes, FriendlyByteBuf::writeUtf);
        buf.writeVarInt(packet.level);
    }

    public static ArchetypeLevelsPacket decode(FriendlyByteBuf buf) {
        return new ArchetypeLevelsPacket(buf.readUUID(), buf.readList(FriendlyByteBuf::readUtf), buf.readVarInt());
    }

    public static void handle(ArchetypeLevelsPacket packet, Supplier<Context> context) {
        context.get()
            .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArchetypes.update(packet.player, packet.archetypes, packet.level)));
        context.get().setPacketHandled(true);
    }
}
