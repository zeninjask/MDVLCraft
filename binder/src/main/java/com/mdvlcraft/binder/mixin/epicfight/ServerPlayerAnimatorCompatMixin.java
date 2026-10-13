package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent;
import yesman.epicfight.compat.PlayerAnimatorCompat;

/**
 * PlayerAnimatorCompat#renderEvent is client-only, so a dedicated server strips it and efs_iss's PlayerAnimatorCompatMixin
 * fails and takes Epic Fight down with it. Applied first (priority 500) on the server only, this puts an empty method back
 * for that injection to find; nothing on the server calls it.
 */
@Mixin(value = PlayerAnimatorCompat.class, priority = 500, remap = false)
public abstract class ServerPlayerAnimatorCompatMixin {
    private void renderEvent(RenderEpicFightPlayerEvent event) {
    }
}
