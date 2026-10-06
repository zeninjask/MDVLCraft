package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.Loadout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: cast key pressed (with the selected wheel slot) or released. */
public record CastPacket(boolean pressed, int slot) {
    public static void encode(CastPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.pressed);
        buf.writeVarInt(packet.slot);
    }

    public static CastPacket decode(FriendlyByteBuf buf) {
        return new CastPacket(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(CastPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!packet.pressed) {
                AbilityActions.release(context.get().getSender());
            } else if (packet.slot >= 0 && packet.slot < Loadout.SLOTS) {
                AbilityActions.press(context.get().getSender(), packet.slot);
            }
        });
        context.get().setPacketHandled(true);
    }
}
