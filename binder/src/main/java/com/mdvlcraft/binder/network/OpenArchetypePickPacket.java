package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.MigrationPopup;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Opens the pop-up where a player from before the vocations rework picks two archetypes (skills/LegacyMigration). */
public record OpenArchetypePickPacket(int level) {
    public static void encode(OpenArchetypePickPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.level);
    }

    public static OpenArchetypePickPacket decode(FriendlyByteBuf buf) {
        return new OpenArchetypePickPacket(buf.readVarInt());
    }

    public static void handle(OpenArchetypePickPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MigrationPopup.open(packet.level)));
        context.get().setPacketHandled(true);
    }
}
