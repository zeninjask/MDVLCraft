package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.GauntClient;
import com.mdvlcraft.binder.seraph.SeraphForm.FormPhase;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** A player's Gaunt Vessel phase, `ticks` into it, or (`cast`) that they have just begun a spell in the form. */
public record GauntFormPacket(int entity, FormPhase phase, int ticks, boolean cast) {
    public static void encode(GauntFormPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeEnum(packet.phase);
        buf.writeVarInt(packet.ticks);
        buf.writeBoolean(packet.cast);
    }

    public static GauntFormPacket decode(FriendlyByteBuf buf) {
        return new GauntFormPacket(buf.readVarInt(), buf.readEnum(FormPhase.class), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(GauntFormPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GauntClient.handle(packet)));
        context.get().setPacketHandled(true);
    }
}
