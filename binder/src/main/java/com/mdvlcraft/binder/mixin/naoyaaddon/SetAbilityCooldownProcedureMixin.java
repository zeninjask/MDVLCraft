package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.procedures.SetAbilityCooldownProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Projection Sorcery's own ability cooldowns are off: the binder's cooldowns (TechniqueCooldowns) are the only ones.
 * The addon put its abilities on a cooldown in Cursed Fate's ability slots when they ended (the dash 30 after every
 * Phantom Movement) and then silently refused the next cast, though Phantom Movement has no cooldown in the binder.
 */
@Mixin(value = SetAbilityCooldownProcedure.class, remap = false)
public abstract class SetAbilityCooldownProcedureMixin {
    @Inject(method = "setCooldownForAbility", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noAddonCooldown(Entity entity, double ability, double cooldown, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "isOnCooldown", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$neverOnAddonCooldown(Entity entity, double ability, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
