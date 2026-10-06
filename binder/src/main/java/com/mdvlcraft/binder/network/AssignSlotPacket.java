package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.Loadout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/** Client to server: put an ability in a wheel slot (empty = clear the slot). */
public record AssignSlotPacket(int slot, Optional<ResourceLocation> ability) {
    public static void encode(AssignSlotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
        buf.writeOptional(packet.ability, FriendlyByteBuf::writeResourceLocation);
    }

    public static AssignSlotPacket decode(FriendlyByteBuf buf) {
        return new AssignSlotPacket(buf.readVarInt(), buf.readOptional(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(AssignSlotPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (packet.slot >= 0 && packet.slot < Loadout.SLOTS) {
                AbilityActions.assign(context.get().getSender(), packet.slot, packet.ability);
            }
        });
        context.get().setPacketHandled(true);
    }
}
