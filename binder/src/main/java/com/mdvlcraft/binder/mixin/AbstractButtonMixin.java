package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.DqGui;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla and Forge text buttons draw as DQ windows (client/DqGui); buttons with their own renderWidget keep theirs. */
@Mixin({AbstractButton.class, ExtendedButton.class})
public abstract class AbstractButtonMixin extends AbstractWidget {
    private AbstractButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$dqButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        DqGui.button(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isHoveredOrFocused(), this.active);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        ((AbstractButton)(Object)this).renderString(graphics, Minecraft.getInstance().font, this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
        ci.cancel();
    }
}
