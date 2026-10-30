package com.mdvlcraft.binder.mixin.debug;

import com.mdvlcraft.binder.util.PacketStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the bytes of every packet sent, by type (util/PacketStats); only with -Dmdvlcraft.packetstats=true. */
@Mixin(PacketEncoder.class)
public abstract class PacketStatsMixin {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("HEAD"))
    private void mdvlcraft$start(ChannelHandlerContext context, Packet<?> packet, ByteBuf out, CallbackInfo ci) {
        if (PacketStats.ON) {
            PacketStats.start(out.writerIndex());
        }
    }

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("RETURN"))
    private void mdvlcraft$count(ChannelHandlerContext context, Packet<?> packet, ByteBuf out, CallbackInfo ci) {
        if (PacketStats.ON) {
            PacketStats.count(packet, out.writerIndex());
        }
    }
}
