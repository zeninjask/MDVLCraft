package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.SkillCalloutClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** A skill's name to call out above a player (ability/SkillCallout). */
public record SkillCalloutPacket(int entityId, Component name, int colour) {
    public static void encode(SkillCalloutPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeComponent(packet.name);
        buf.writeInt(packet.colour);
    }

    public static SkillCalloutPacket decode(FriendlyByteBuf buf) {
        return new SkillCalloutPacket(buf.readVarInt(), buf.readComponent(), buf.readInt());
    }

    public static void handle(SkillCalloutPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkillCalloutClient.show(packet.entityId, packet.name, packet.colour)));
        context.get().setPacketHandled(true);
    }
}
