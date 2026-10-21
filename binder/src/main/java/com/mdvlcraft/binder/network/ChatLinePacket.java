package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.client.TypedChat;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** One chat line: the speaker part ("Name - Title: ") shows at once, the message is typed out (client/TypedChat). */
public record ChatLinePacket(Component prefix, Component message) {
    public static void encode(ChatLinePacket packet, FriendlyByteBuf buf) {
        buf.writeComponent(packet.prefix);
        buf.writeComponent(packet.message);
    }

    public static ChatLinePacket decode(FriendlyByteBuf buf) {
        return new ChatLinePacket(buf.readComponent(), buf.readComponent());
    }

    public static void handle(ChatLinePacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TypedChat.show(packet.prefix, packet.message)));
        context.get().setPacketHandled(true);
    }
}
