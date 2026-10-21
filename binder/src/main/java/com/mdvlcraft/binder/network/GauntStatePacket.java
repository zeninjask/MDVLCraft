package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.GauntClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * A Haunted Lord's leap state (gaunt/GauntLeap): ticks into a charge (-1 none), the face of the block it clings to
 * (Direction 3D data value, -1 none), whether it is mid-leap, the ticks until it can leap, horrify, execute and go faceless again, and whether its guard is up.
 */
public record GauntStatePacket(int entity, int charging, int cling, boolean leaping, int cooldown, int horrifyCooldown, int executeCooldown, int facelessCooldown, boolean blocking) {
    public static void encode(GauntStatePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeVarInt(packet.charging);
        buf.writeVarInt(packet.cling);
        buf.writeBoolean(packet.leaping);
        buf.writeVarInt(packet.cooldown);
        buf.writeVarInt(packet.horrifyCooldown);
        buf.writeVarInt(packet.executeCooldown);
        buf.writeVarInt(packet.facelessCooldown);
        buf.writeBoolean(packet.blocking);
    }

    public static GauntStatePacket decode(FriendlyByteBuf buf) {
        return new GauntStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(GauntStatePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GauntClient.handle(packet)));
        context.get().setPacketHandled(true);
    }
}
