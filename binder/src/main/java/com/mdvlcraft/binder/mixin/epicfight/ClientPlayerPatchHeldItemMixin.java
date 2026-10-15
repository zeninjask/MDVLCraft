package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.wukong.WukongGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** The client's held-item change (living motions, the item's animations) with the Wukong gate (wukong/WukongGate). */
@Mixin(value = {AbstractClientPlayerPatch.class, LocalPlayerPatch.class}, remap = false)
public abstract class ClientPlayerPatchHeldItemMixin {
    @ModifyVariable(method = "updateHeldItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private CapabilityItem mdvlcraft$gateFrom(CapabilityItem from) {
        return WukongGate.gate((LivingEntityPatch<?>)(Object)this, from);
    }

    @ModifyVariable(method = "updateHeldItem", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private CapabilityItem mdvlcraft$gateTo(CapabilityItem to) {
        return WukongGate.gate((LivingEntityPatch<?>)(Object)this, to);
    }
}
