package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.Traits;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hungry Mind: hunger drains 15% faster; Second Breakfast: 10% faster. */
@Mixin(Player.class)
public abstract class ExhaustionMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float mdvlcraft$hungryMind(float exhaustion) {
        Player player = (Player)(Object)this;
        float rate = Traits.has(player, Trait.HUNGRY_MIND) ? 1.15F : Traits.has(player, Trait.SECOND_BREAKFAST) ? 1.1F : 1.0F;
        return exhaustion * rate;
    }
}
