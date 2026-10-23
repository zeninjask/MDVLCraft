package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.shadow.ShadowArsenal;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The sword wheel's choice: the weapon (an item id) Formless Sword's Form mode summons. */
public record PickShadowPacket(String item) {
    public static void encode(PickShadowPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.item);
    }

    public static PickShadowPacket decode(FriendlyByteBuf buf) {
        return new PickShadowPacket(buf.readUtf());
    }

    public static void handle(PickShadowPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> ShadowArsenal.pick(context.get().getSender(), packet.item));
        context.get().setPacketHandled(true);
    }
}
