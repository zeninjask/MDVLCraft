package com.mdvlcraft.binder.mixin.ares;

import com.super_awesome_baby.ares_hud.client.gui.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Ares HUD's value strip draws its icons 6 px tall; the Dragon Quest IX icons the DQ pack gives it are 32 px with the
 * item inside the middle 24, so at 8 px they map 1:1 at GUI scale 4 and the item fills the strip's 6 px inside.
 */
@Mixin(value = RenderUtils.class, remap = false)
public abstract class RenderUtilsMixin {
    @ModifyConstant(
        method = "renderCenteredIconAndText(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;IIII)V",
        constant = @Constant(floatValue = 6.0F),
        require = 1
    )
    private static float mdvlcraft$iconSize(float size) {
        return 8.0F;
    }
}
