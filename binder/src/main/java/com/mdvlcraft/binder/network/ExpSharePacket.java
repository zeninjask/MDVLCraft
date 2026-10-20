package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.ExpShareButton;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** The Experience Share setting the player follows and whether they may change it, for the Social Interactions screen's button (content/ExpShare). */
public record ExpSharePacket(boolean on, boolean decides) {
    public static void encode(ExpSharePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.on);
        buf.writeBoolean(packet.decides);
    }

    public static ExpSharePacket decode(FriendlyByteBuf buf) {
        return new ExpSharePacket(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ExpSharePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpShareButton.set(packet.on, packet.decides)));
        context.get().setPacketHandled(true);
    }
}
