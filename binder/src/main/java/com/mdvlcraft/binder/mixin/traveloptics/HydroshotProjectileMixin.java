package com.mdvlcraft.binder.mixin.traveloptics;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Traveloptics' Hydroshot returns its impact sound as an Optional of a bare SoundEvent, but Iron's Spells 3.16 reads
 * getImpactSound() as an Optional of a Supplier and casts it on impact, which crashed the server when a Hydroshot hit.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.hydroshot.HydroshotProjectile", remap = false)
public abstract class HydroshotProjectileMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getImpactSound", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$supplySound(CallbackInfoReturnable<Optional> cir) {
        Optional<SoundEvent> sound = cir.getReturnValue();
        cir.setReturnValue(sound.map(event -> (Supplier<SoundEvent>)() -> event));
    }
}
