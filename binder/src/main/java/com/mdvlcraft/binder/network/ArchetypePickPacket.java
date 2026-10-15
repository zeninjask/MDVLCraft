package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.skills.LegacyMigration;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The migration pop-up's two archetypes (skills/LegacyMigration.pick). */
public record ArchetypePickPacket(String first, String second) {
    public static void encode(ArchetypePickPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.first, 32);
        buf.writeUtf(packet.second, 32);
    }

    public static ArchetypePickPacket decode(FriendlyByteBuf buf) {
        return new ArchetypePickPacket(buf.readUtf(32), buf.readUtf(32));
    }

    public static void handle(ArchetypePickPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                LegacyMigration.pick(context.get().getSender(), packet.first, packet.second);
            }
        });
        context.get().setPacketHandled(true);
    }
}
