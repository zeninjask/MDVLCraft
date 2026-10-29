package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.traits.TraitSlots;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** The slot machine's answer: the slot (0-based) to put the rolled trait in, or -1 to throw it away. */
public record TraitChoicePacket(int slot) {
    public static void encode(TraitChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
    }

    public static TraitChoicePacket decode(FriendlyByteBuf buf) {
        return new TraitChoicePacket(buf.readVarInt());
    }

    public static void handle(TraitChoicePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                TraitSlots.choose(player, packet.slot);
            }
        });
        context.get().setPacketHandled(true);
    }
}
