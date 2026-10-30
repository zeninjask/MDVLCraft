package com.mdvlcraft.binder.mixin.debug;

import com.mdvlcraft.binder.util.PacketStats;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logs who sends a packet type named by -Dmdvlcraft.packettrace (util/PacketStats). */
@Mixin(Connection.class)
public abstract class PacketTraceMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"))
    private void mdvlcraft$trace(Packet<?> packet, net.minecraft.network.PacketSendListener listener, CallbackInfo ci) {
        if (PacketStats.TRACE != null) {
            PacketStats.trace(packet);
        }
    }
}
