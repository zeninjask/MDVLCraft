package com.mdvlcraft.binder.mixin.parties;

import com.mdvlcraft.binder.combat.InvisibleUntargetable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An invisible party member glows (outline) for the rest of their party, and only for them. */
@Mixin(Minecraft.class)
public abstract class InvisibleAllyGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$invisibleAllyGlows(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (minecraft.player != null && entity != minecraft.player && entity instanceof LivingEntity living
            && InvisibleUntargetable.hidden(living) && minecraft.player.isAlliedTo(entity)) {
            cir.setReturnValue(true);
        }
    }
}
