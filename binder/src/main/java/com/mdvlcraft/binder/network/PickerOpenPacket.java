package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.skills.PickerShield;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The client's class picker (client/picker/PickerScreen) opened or closed (skills/PickerShield). */
public record PickerOpenPacket(boolean open) {
    public static void encode(PickerOpenPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.open);
    }

    public static PickerOpenPacket decode(FriendlyByteBuf buf) {
        return new PickerOpenPacket(buf.readBoolean());
    }

    public static void handle(PickerOpenPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                PickerShield.setOpen(context.get().getSender(), packet.open);
            }
        });
        context.get().setPacketHandled(true);
    }
}
