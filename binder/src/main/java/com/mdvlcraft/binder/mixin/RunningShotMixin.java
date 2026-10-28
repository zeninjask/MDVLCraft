package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.specs.Specs;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Running Shot (specs): drawing a bow while sprinting doesn't slow the player (the 0.2 use-item slowdown). */
@Mixin(LocalPlayer.class)
public abstract class RunningShotMixin {
    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.2F))
    private float mdvlcraft$runningShot(float slowdown) {
        LocalPlayer self = (LocalPlayer)(Object)this;
        return self.isUsingItem() && self.isSprinting() && self.getUseItem().getItem() instanceof BowItem && Specs.has(self, "running_shot")
            ? 1.0F : slowdown;
    }
}
