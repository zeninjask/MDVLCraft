package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.Traits;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ghost Walker: a sprinting ghost walker and the creatures it runs into don't push each other (both sides run this). */
@Mixin(Entity.class)
public abstract class GhostWalkerPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$ghostWalker(Entity other, CallbackInfo ci) {
        if (ghost((Entity)(Object)this) || ghost(other)) {
            ci.cancel();
        }
    }

    private static boolean ghost(Entity entity) {
        return entity instanceof Player player && player.isSprinting() && Traits.has(player, Trait.GHOST_WALKER);
    }
}
