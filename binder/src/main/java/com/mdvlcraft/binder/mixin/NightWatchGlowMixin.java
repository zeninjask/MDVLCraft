package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.specs.Specs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Night Watch (specs): at night, enemies within 12 blocks glow for the player (seen through walls). */
@Mixin(Minecraft.class)
public abstract class NightWatchGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$nightWatch(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (minecraft.player != null && entity instanceof Enemy && entity.distanceToSqr(minecraft.player) <= 144.0) {
            long time = minecraft.player.level().getDayTime() % 24000L;
            if (time >= 13000L && time <= 23000L && Specs.has(minecraft.player, "night_watch")) {
                cir.setReturnValue(true);
            }
        }
    }
}
