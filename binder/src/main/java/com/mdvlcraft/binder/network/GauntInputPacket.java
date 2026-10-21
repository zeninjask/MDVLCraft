package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.gaunt.GauntLeap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent.Context;

/** The client's Haunted Lord dodged (DODGE, steering strafe/forward) or raised / lowered its guard (GUARD_ON / GUARD_OFF). */
public record GauntInputPacket(int action, float strafe, float forward) {
    public static final int DODGE = 0;
    public static final int GUARD_ON = 1;
    public static final int GUARD_OFF = 2;

    public static void encode(GauntInputPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.action);
        buf.writeFloat(packet.strafe);
        buf.writeFloat(packet.forward);
    }

    public static GauntInputPacket decode(FriendlyByteBuf buf) {
        return new GauntInputPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(GauntInputPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            switch (packet.action) {
                case DODGE -> GauntLeap.dodge(context.get().getSender(), Mth.clamp(packet.strafe, -1.0F, 1.0F), Mth.clamp(packet.forward, -1.0F, 1.0F));
                case GUARD_ON -> GauntLeap.block(context.get().getSender(), true);
                case GUARD_OFF -> GauntLeap.block(context.get().getSender(), false);
                default -> {
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
