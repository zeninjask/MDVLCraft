package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.ability.SilentAppraisal;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Silent Appraisal's crow camera is tracked as far as the server's view distance allows (SilentAppraisal). */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @Inject(method = "clientTrackingRange", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$farCrowCamera(CallbackInfoReturnable<Integer> cir) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey((EntityType<?>)(Object)this);
        if (key != null && SilentAppraisal.CAMERA_TYPE.equals(key.toString())) {
            cir.setReturnValue(64);
        }
    }
}
