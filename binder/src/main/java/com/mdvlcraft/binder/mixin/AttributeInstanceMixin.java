package com.mdvlcraft.binder.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removing a modifier an attribute doesn't have still marked it changed, so it was sent again to the player and everyone
 * near them. Caelus removes its elytra modifier from every player every tick (re-adding it only for an elytra), which kept
 * caelus:fall_flying going out ten times a second per player. A modifier that isn't there is now simply not removed.
 */
@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin {
    @Shadow
    public abstract AttributeModifier getModifier(java.util.UUID id);

    @Inject(method = "removeModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$absent(AttributeModifier modifier, CallbackInfo ci) {
        if (this.getModifier(modifier.getId()) == null) {
            ci.cancel();
        }
    }
}
