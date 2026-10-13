package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Parties only cancels damage events between party members (friendlyFire = false). Spells and abilities pick their
 * targets with Entity.isAlliedTo, so party members still caught each other in area spells, roots and slows. This makes
 * isAlliedTo ask Parties too. Parties' vanilla provider calls isAlliedTo itself, so a re-entry guard lets that inner call
 * fall through to vanilla.
 */
@Mixin(Entity.class)
public abstract class EntityAllianceMixin {
    @Unique
    private static final ThreadLocal<Boolean> mdvlcraft$asking = ThreadLocal.withInitial(() -> false);

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$partyMembersAreAllies(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (mdvlcraft$asking.get()) {
            return;
        }

        mdvlcraft$asking.set(true);
        try {
            if (AllianceAPI.areAllied((Entity)(Object)this, other)) {
                cir.setReturnValue(true);
            }
        } finally {
            mdvlcraft$asking.set(false);
        }
    }
}
