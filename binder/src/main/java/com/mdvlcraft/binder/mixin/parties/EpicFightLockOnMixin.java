package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties.common.group.ClientDataHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Epic Fight's lock-on (also used by Better Lock On) can target any living entity except one on your scoreboard team.
 * Party members can't be locked on to either. The client doesn't receive Parties' friendly-fire game rule, so this
 * checks Parties' client party list directly.
 */
@Mixin(targets = "yesman.epicfight.api.client.camera.EpicFightCameraAPI", remap = false)
public abstract class EpicFightLockOnMixin {
    @Inject(method = "predicateFocusableEntity", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$skipPartyMembers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof Player player && ClientDataHandler.getWrapper(player.getUUID()) != null) {
            cir.setReturnValue(false);
        }
    }
}
