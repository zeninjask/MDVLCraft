package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.util.CrashGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** An error while a player ticks on the server (their player tick events among it) is logged instead of crashing it. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerTickGuardMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"), require = 0)
    private void mdvlcraft$guard(ServerPlayer player) {
        try {
            player.doTick();
        } catch (Throwable error) {
            if (!CrashGuard.catchable(error)) {
                throw error;
            }
            CrashGuard.caught("player tick", CrashGuard.describe(player), error);
        }
    }
}
