package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.SeraphClient;
import com.mdvlcraft.binder.seraph.SeraphForm.FormPhase;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** A player's Seraph Embodiment phase, `ticks` into it, or (`cast`) that they have just begun a spell in the form. */
public record SeraphFormPacket(int entity, FormPhase phase, int ticks, boolean cast) {
    public static void encode(SeraphFormPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeEnum(packet.phase);
        buf.writeVarInt(packet.ticks);
        buf.writeBoolean(packet.cast);
    }

    public static SeraphFormPacket decode(FriendlyByteBuf buf) {
        return new SeraphFormPacket(buf.readVarInt(), buf.readEnum(FormPhase.class), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SeraphFormPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SeraphClient.handle(packet)));
        context.get().setPacketHandled(true);
    }
}
