package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ExpShareButton;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player's Experience Share setting, for the Social Interactions screen's button (content/ExpShare). */
public record ExpSharePacket(boolean on) {
    public static void encode(ExpSharePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.on);
    }

    public static ExpSharePacket decode(FriendlyByteBuf buf) {
        return new ExpSharePacket(buf.readBoolean());
    }

    public static void handle(ExpSharePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpShareButton.set(packet.on)));
        context.get().setPacketHandled(true);
    }
}
