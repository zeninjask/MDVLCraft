package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.DqGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla sliders: a DQ window for the track and a blue key for the handle (client/DqGui). */
@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget {
    private AbstractSliderButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(
        method = "renderWidget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIII)V",
            ordinal = 0
        )
    )
    private void mdvlcraft$dqTrack(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h, int sliceW, int sliceH, int uw, int vh, int u, int v) {
        DqGui.button(graphics, x, y, w, h, false, this.active);
    }

    @Redirect(
        method = "renderWidget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIII)V",
            ordinal = 1
        )
    )
    private void mdvlcraft$dqHandle(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h, int sliceW, int sliceH, int uw, int vh, int u, int v) {
        DqGui.handle(graphics, x, y, w, h, this.isHoveredOrFocused());
    }
}
