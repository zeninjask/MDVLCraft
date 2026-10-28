package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.util.CrashGuard;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** An error while an entity ticks (on the server or the client) is logged by CrashGuard instead of crashing the game. */
@Mixin(Level.class)
public abstract class EntityTickGuardMixin {
    @ModifyVariable(method = "guardEntityTick", at = @At("HEAD"), argsOnly = true, require = 0)
    private <T extends Entity> Consumer<T> mdvlcraft$guard(Consumer<T> tick) {
        return entity -> {
            try {
                tick.accept(entity);
                CrashGuard.entityTicked(entity);
            } catch (Throwable error) {
                if (!CrashGuard.catchable(error)) {
                    throw error;
                }
                CrashGuard.entityTickFailed(entity, error);
            }
        };
    }
}
