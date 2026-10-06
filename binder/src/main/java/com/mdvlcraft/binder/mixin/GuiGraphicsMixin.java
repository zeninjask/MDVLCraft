package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.SlateRecruits;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every string GuiGraphics draws passes its colour through SlateRecruits (light labels on Recruits' Slate screens). */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(method = {
            "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
    }, at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int mdvlcraft$slateText(int colour) {
        return SlateRecruits.textColour(colour);
    }
}
