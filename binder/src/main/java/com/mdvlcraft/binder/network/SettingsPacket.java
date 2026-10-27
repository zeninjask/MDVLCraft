package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.KeepSlotOpen;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The Settings screen changed the player's settings (client/SettingsScreen). */
public record SettingsPacket(boolean keepSlotOpen) {
    public static void encode(SettingsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.keepSlotOpen);
    }

    public static SettingsPacket decode(FriendlyByteBuf buf) {
        return new SettingsPacket(buf.readBoolean());
    }

    public static void handle(SettingsPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> KeepSlotOpen.set(context.get().getSender(), packet.keepSlotOpen));
        context.get().setPacketHandled(true);
    }
}
