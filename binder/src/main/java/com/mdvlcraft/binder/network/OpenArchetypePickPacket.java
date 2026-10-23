package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.MigrationPopup;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Opens the pop-up where a player picks two archetypes: a player from before the vocations rework (skills/LegacyMigration),
 * or a free respec offered by an admin (free, Respec.offerFree).
 */
public record OpenArchetypePickPacket(int level, boolean free) {
    public static void encode(OpenArchetypePickPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.level);
        buf.writeBoolean(packet.free);
    }

    public static OpenArchetypePickPacket decode(FriendlyByteBuf buf) {
        return new OpenArchetypePickPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(OpenArchetypePickPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MigrationPopup.open(packet.level, packet.free)));
        context.get().setPacketHandled(true);
    }
}
