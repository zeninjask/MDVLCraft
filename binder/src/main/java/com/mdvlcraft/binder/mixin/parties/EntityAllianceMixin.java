package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.modified.Summons;
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
 *
 * A Modified ability's summon (Summons) is on its owner's side whatever else says: without this a summoned vindicator
 * still counted as allied with pillagers and never targeted them. Priority 100 puts this ahead of other mods' checks.
 */
@Mixin(value = Entity.class, priority = 100)
public abstract class EntityAllianceMixin {
    @Unique
    private static final ThreadLocal<Boolean> mdvlcraft$asking = ThreadLocal.withInitial(() -> false);

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$partyMembersAreAllies(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (other != null && (!Summons.kind(self).isEmpty() || !Summons.kind(other).isEmpty())) {
            cir.setReturnValue(Summons.allied(self, other));
            return;
        }
        if (mdvlcraft$asking.get()) {
            return;
        }

        mdvlcraft$asking.set(true);
        try {
            if (AllianceAPI.areAllied(self, other)) {
                cir.setReturnValue(true);
            }
        } finally {
            mdvlcraft$asking.set(false);
        }
    }
}
