package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.content.SummonKills;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remembers the killing damage source while Pufferfish hands out kill experience (its injection at the end of
 * dropAllDeathLoot), for SummonKills. The priority puts the clearing after Pufferfish's injection.
 */
@Mixin(value = LivingEntity.class, priority = 2000)
public abstract class SummonKillSourceMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void mdvlcraft$rememberSource(DamageSource source, CallbackInfo ci) {
        SummonKills.dying(source);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void mdvlcraft$forgetSource(DamageSource source, CallbackInfo ci) {
        SummonKills.died();
    }
}
