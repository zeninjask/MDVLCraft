package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.network.ToggleProjectionStylePacket;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** The projection style stays Naobito's (ProjectionSorcery sets it); the addon's style toggle does nothing. */
@Mixin(value = ToggleProjectionStylePacket.class, remap = false)
public abstract class ToggleProjectionStylePacketMixin {
    @Inject(method = "handler", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$naobitoOnly(ToggleProjectionStylePacket packet, Supplier<NetworkEvent.Context> context, CallbackInfo ci) {
        context.get().setPacketHandled(true);
        ci.cancel();
    }
}
