package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPChangeSkill;

import java.util.function.Supplier;

/**
 * Epic Fight's equip packet teaches and equips whatever skill the client names, unchecked. Skills are learned through
 * the skill trees and equipped through the binder's own packet (EquipSkillPacket), so this one is ignored.
 */
@Mixin(value = CPChangeSkill.class, remap = false)
public abstract class CPChangeSkillMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$ignore(CPChangeSkill msg, Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        ctx.get().setPacketHandled(true);
        ci.cancel();
    }
}
