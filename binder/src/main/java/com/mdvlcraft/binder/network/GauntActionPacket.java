package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.GauntClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * A Haunted Lord began one of its own animations `ticks` ago (gaunt/GauntAttack, gaunt/GauntExecute; "" for none): for an
 * execution also the victim held (entity id, -1 none or let go) and the way the Haunted Lord faces through it (NaN for
 * the player's own).
 */
public record GauntActionPacket(int entity, String animation, int ticks, int victim, float yaw) {
    public static GauntActionPacket none(int entity) {
        return new GauntActionPacket(entity, "", 0, -1, Float.NaN);
    }

    public static void encode(GauntActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.animation, 32);
        buf.writeVarInt(packet.ticks);
        buf.writeVarInt(packet.victim);
        buf.writeFloat(packet.yaw);
    }

    public static GauntActionPacket decode(FriendlyByteBuf buf) {
        return new GauntActionPacket(buf.readVarInt(), buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static void handle(GauntActionPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GauntClient.handle(packet)));
        context.get().setPacketHandled(true);
    }
}
