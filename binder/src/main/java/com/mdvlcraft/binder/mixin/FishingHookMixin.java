package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.OriginTraits;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Fisher's Angler's Patience works like one more level of Lure. */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow @Final @Mutable
    private int lureSpeed;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("TAIL"))
    private void mdvlcraft$anglersPatience(Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
        this.lureSpeed = OriginTraits.lure(player, this.lureSpeed)
            + (com.mdvlcraft.binder.traits.Traits.has(player, com.mdvlcraft.binder.traits.Trait.ANGLERS_LUCK) ? 1 : 0);   // Angler's Luck: one more level of Lure
    }
}
