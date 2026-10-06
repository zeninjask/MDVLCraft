package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.DisabledContent;
import net.minecraft.network.Connection;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The server ignores every client-to-server packet on a disabled mod's channels. For Cursed Fate these are its ability,
 * menu, combat-mode, output, quest/trade buttons, key-input, left-click, dash and roll packets; its techniques are only
 * started by the binder on the server. Server-to-client sync packets are untouched.
 */
@Mixin(value = NetworkHooks.class, remap = false)
public abstract class NetworkHooksMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$dropDisabledServerbound(ICustomPacket<?> packet, Connection connection, CallbackInfoReturnable<Boolean> cir) {
        if (packet.getDirection() == NetworkDirection.PLAY_TO_SERVER && DisabledContent.isDisabled(packet.getName())) {
            cir.setReturnValue(true);
        }
    }
}
