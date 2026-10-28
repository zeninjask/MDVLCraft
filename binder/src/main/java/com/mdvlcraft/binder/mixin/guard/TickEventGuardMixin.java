package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.util.CrashGuard;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * An error in any mod's handler of a tick event (server, level, client, render or player tick) is logged instead of
 * crashing the game; the handlers after it miss that one tick.
 */
@Mixin(value = ForgeEventFactory.class, remap = false)
public abstract class TickEventGuardMixin {
    @Redirect(method = {"onPreServerTick", "onPostServerTick", "onPreLevelTick", "onPostLevelTick", "onPreClientTick", "onPostClientTick",
        "onRenderTickStart", "onRenderTickEnd", "onPlayerPreTick", "onPlayerPostTick"},
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"), require = 0)
    private static boolean mdvlcraft$guard(IEventBus bus, Event event) {
        try {
            return bus.post(event);
        } catch (Throwable error) {
            if (!CrashGuard.catchable(error)) {
                throw error;
            }
            CrashGuard.caught("tick event", event.getClass().getName(), error);
            return false;
        }
    }
}
