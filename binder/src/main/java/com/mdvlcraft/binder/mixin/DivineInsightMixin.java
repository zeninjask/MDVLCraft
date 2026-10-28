package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.specs.Specs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Divine Insight (specs): enchanting costs one level less. */
@Mixin(Player.class)
public abstract class DivineInsightMixin {
    @ModifyVariable(method = "onEnchantmentPerformed", at = @At("HEAD"), argsOnly = true)
    private int mdvlcraft$divineInsight(int levels) {
        return Specs.has((Player)(Object)this, "divine_insight") ? Math.max(0, levels - 1) : levels;
    }
}
