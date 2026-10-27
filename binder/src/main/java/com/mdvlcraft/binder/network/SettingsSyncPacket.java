package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.content.KeepSlotOpen;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player's settings, for the client's slot guards and the Settings screen (content/KeepSlotOpen). */
public record SettingsSyncPacket(boolean keepSlotOpen) {
    public static void encode(SettingsSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.keepSlotOpen);
    }

    public static SettingsSyncPacket decode(FriendlyByteBuf buf) {
        return new SettingsSyncPacket(buf.readBoolean());
    }

    public static void handle(SettingsSyncPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> KeepSlotOpen.setClient(packet.keepSlotOpen));
        context.get().setPacketHandled(true);
    }
}
