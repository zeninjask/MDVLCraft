package com.mdvlcraft.binder.client;

import net.minecraft.client.gui.GuiGraphics;

/** The ability HUD's look: dark slate boxes with 2px-cut corners. Screens use DqGui. */
final class SlateGui {
    private SlateGui() {
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        box(graphics, x, y, width, height, -300607458, -13486014);
    }

    static void slot(GuiGraphics graphics, int x, int y, int size) {
        box(graphics, x, y, size, size, -14933977, -12959670);
    }

    static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int colour) {
        graphics.fill(x, y, x + width, y + height, -14144202);
        graphics.fill(x, y, x + Math.round(width * fraction), y + height, colour);
    }

    private static void box(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x + 2, y, right - 2, y + 1, border);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, border);
        graphics.fill(x, y + 2, x + 1, bottom - 2, border);
        graphics.fill(right - 1, y + 2, right, bottom - 2, border);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, border);
        graphics.fill(right - 2, y + 1, right - 1, y + 2, border);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, border);
        graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, border);
        graphics.fill(x + 2, y + 1, right - 2, y + 2, fill);
        graphics.fill(x + 1, y + 2, right - 1, bottom - 2, fill);
        graphics.fill(x + 2, bottom - 2, right - 2, bottom - 1, fill);
    }
}
