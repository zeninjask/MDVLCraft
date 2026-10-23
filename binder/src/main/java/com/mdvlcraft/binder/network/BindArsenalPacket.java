package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.shadow.ShadowArsenal;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The arsenal screen binds an arsenal weapon (an item id) to a sword wheel slot, or clears it. */
public record BindArsenalPacket(int slot, Optional<String> item) {
    public static void encode(BindArsenalPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
        buf.writeOptional(packet.item, FriendlyByteBuf::writeUtf);
    }

    public static BindArsenalPacket decode(FriendlyByteBuf buf) {
        return new BindArsenalPacket(buf.readVarInt(), buf.readOptional(FriendlyByteBuf::readUtf));
    }

    public static void handle(BindArsenalPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> ShadowArsenal.bind(context.get().getSender(), packet.slot, packet.item));
        context.get().setPacketHandled(true);
    }
}
