package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.wukong.WukongGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** A held-item change sets the innate skill from the item's capability: the Ruyi Jingu Bang's only with the Monkey King identity. */
@Mixin(value = ServerPlayerPatch.class, remap = false)
public abstract class ServerPlayerPatchHeldItemMixin {
    @ModifyVariable(method = "updateHeldItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private CapabilityItem mdvlcraft$gateFrom(CapabilityItem from) {
        return WukongGate.gate((ServerPlayerPatch)(Object)this, from);
    }

    @ModifyVariable(method = "updateHeldItem", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private CapabilityItem mdvlcraft$gateTo(CapabilityItem to) {
        return WukongGate.gate((ServerPlayerPatch)(Object)this, to);
    }
}
