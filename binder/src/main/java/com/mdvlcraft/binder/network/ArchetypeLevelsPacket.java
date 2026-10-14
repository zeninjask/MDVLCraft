package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ClientArchetypes;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * A player's picked archetypes, highest level first, that highest level, the experience into it out of the experience
 * it needs (0 needed: the last level), and the experience it just gained (0: nothing new), for the party windows.
 */
public record ArchetypeLevelsPacket(UUID player, List<String> archetypes, int level, int current, int required, int gained) {
    public static void encode(ArchetypeLevelsPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeCollection(packet.archetypes, FriendlyByteBuf::writeUtf);
        buf.writeVarInt(packet.level);
        buf.writeVarInt(packet.current);
        buf.writeVarInt(packet.required);
        buf.writeVarInt(packet.gained);
    }

    public static ArchetypeLevelsPacket decode(FriendlyByteBuf buf) {
        return new ArchetypeLevelsPacket(
            buf.readUUID(), buf.readList(FriendlyByteBuf::readUtf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
        );
    }

    public static void handle(ArchetypeLevelsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArchetypes.update(packet)));
        context.get().setPacketHandled(true);
    }

    /** The same state with no gain, as sent to a player who logs in. */
    public ArchetypeLevelsPacket withoutGain() {
        return new ArchetypeLevelsPacket(this.player, this.archetypes, this.level, this.current, this.required, 0);
    }
}
