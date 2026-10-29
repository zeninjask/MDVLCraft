package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.Traits;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hungry Mind: hunger drains 15% faster. */
@Mixin(Player.class)
public abstract class ExhaustionMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float mdvlcraft$hungryMind(float exhaustion) {
        return Traits.has((Player)(Object)this, Trait.HUNGRY_MIND) ? exhaustion * 1.15F : exhaustion;
    }
}
