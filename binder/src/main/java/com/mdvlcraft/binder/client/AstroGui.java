package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AstroGui {
    public static final ResourceLocation SKY = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/astro_sky.png");
    public static final int NAVY = -267249868;
    public static final int GOLD = -5600176;
    public static final int GOLD_FAINT = 1621789776;
    public static final int HEADING = -2047366;
    public static final int LABEL = -2693889;
    public static final int VALUE = -1;
    public static final int MUTED = -7562556;

    private AstroGui() {
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, -267249868);

        for (int tx = 0; tx < w; tx += 128) {
            for (int ty = 0; ty < h; ty += 128) {
                g.blit(SKY, x + tx, y + ty, 0.0F, 0.0F, Math.min(128, w - tx), Math.min(128, h - ty), 128, 128);
            }
        }

        frame(g, x, y, w, h, -5600176);
        frame(g, x + 3, y + 3, w - 6, h - 6, 1621789776);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    public static class Button extends AbstractButton {
        private final Runnable action;
        protected int labelOffset;

        public Button(int x, int y, int w, int h, Component label, Runnable action) {
            super(x, y, w, h, label);
            this.action = action;
        }

        @Override
        public void onPress() {
            this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hot = this.isHoveredOrFocused() && this.active;
            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, hot ? -265932700 : -267118020);
            AstroGui.frame(g, this.getX(), this.getY(), this.width, this.height, hot ? -2047366 : -5600176);
            int colour = !this.active ? -7562556 : (hot ? -2047366 : -2693889);
            g.drawCenteredString(
                Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2 + this.labelOffset, this.getY() + (this.height - 8) / 2, colour
            );
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
