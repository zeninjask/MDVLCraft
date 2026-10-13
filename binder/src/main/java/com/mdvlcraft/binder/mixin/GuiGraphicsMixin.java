package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.DqRecruits;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(
        method = {
                "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
                "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
        },
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private int mdvlcraft$recruitsText(int colour) {
        return DqRecruits.textColour(colour);
    }

    @ModifyVariable(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"), ordinal = 5, argsOnly = true)
    private int mdvlcraft$recruitsFill(int colour) {
        return DqRecruits.fillColour(colour);
    }
}
