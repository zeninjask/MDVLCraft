package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.gaunt.GauntAttack;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/** The client's Haunted Lord pressed attack (gaunt/GauntAttack). */
public record GauntAttackPacket() {
    public static void encode(GauntAttackPacket packet, FriendlyByteBuf buf) {
    }

    public static GauntAttackPacket decode(FriendlyByteBuf buf) {
        return new GauntAttackPacket();
    }

    public static void handle(GauntAttackPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> GauntAttack.press(context.get().getSender()));
        context.get().setPacketHandled(true);
    }
}
