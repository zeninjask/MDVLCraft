package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties.common.group.ClientDataHandler;
import com.mdvlcraft.binder.client.ClientSummonOwners;
import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Epic Fight's lock-on (also used by Better Lock On) can target any living entity except one on your scoreboard team.
 * Party members can't be locked on to either, nor can summons (Iron's summons, doppelgangers, tamed animals) owned by
 * you or a party member. The client doesn't receive Parties' friendly-fire game rule, so this checks Parties' client
 * party list directly.
 */
@Mixin(targets = "yesman.epicfight.api.client.camera.EpicFightCameraAPI", remap = false)
public abstract class EpicFightLockOnMixin {
    @Inject(method = "predicateFocusableEntity", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$skipFriendly(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        UUID friend = entity instanceof Player player ? player.getUUID() : mdvlcraft$owner(entity);
        if (friend != null && (friend.equals(Minecraft.getInstance().player.getUUID()) || ClientDataHandler.getWrapper(friend) != null)) {
            cir.setReturnValue(false);
        }
    }

    private static UUID mdvlcraft$owner(Entity entity) {
        if (entity instanceof IMagicSummon) {
            return ClientSummonOwners.get(entity);
        }
        if (entity instanceof DoppelgangerEntity doppelganger) {
            return doppelganger.ownerId().orElse(null);
        }
        if (entity instanceof OwnableEntity ownable) {
            return ownable.getOwnerUUID();
        }
        return null;
    }
}
