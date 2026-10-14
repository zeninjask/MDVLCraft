package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.BattleModeGui;

/** Epic Fight's own stamina bar (bottom right in battle mode) is hidden; Ares HUD already shows stamina. */
@Mixin(value = BattleModeGui.class, remap = false)
public abstract class StaminaBarMixin {
    @Inject(method = "renderStaminaBar", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$hide(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        ci.cancel();
    }
}
