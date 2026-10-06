package com.mdvlcraft.binder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.puffish.skillsmod.client.gui.SkillsScreen;

/**
 * Slate GUI pieces shared by the binder's screens and HUD: the charcoal panels, hairline borders and red accent of
 * the skill screen (tools/generate_textures.py). Boxes have 2px-cut corners like the skill frames.
 */
final class SlateGui {
    static final int TEXT = 0xFFE8ECF2;
    static final int MUTED = 0xFF8892A0;
    static final int ACCENT = 0xFFE25440;
    static final int MANA = 0xFF6E78FF;
    static final int CAST = 0xFFF0C448;
    /** Shade over an ability the player has not unlocked (level 0). */
    static final int UNAVAILABLE = 0xB0401010;
    private static final int TOOLTIP = 0xF8121419;
    private static final int TOOLTIP_BORDER = 0xFF3C424E;
    private static final int PANEL = 0xEE15181E;
    private static final int BORDER = 0xFF323842;
    private static final int TRACK = 0xFF282D36;
    private static final int SLOT = 0xFF1C2027;
    private static final int SLOT_PICKED = 0xFF57302E;
    private static final int SLOT_HOVERED = 0xFF282D36;
    private static final int SLOT_BORDER = 0xFF3A404A;
    private static final int SLOT_EMPTY_BORDER = 0xFF2C3139;

    enum Slot { EMPTY, FILLED, PICKED }

    enum Button { NORMAL, HOVERED, INACTIVE }

    private SlateGui() {
    }

    /** Tooltips on the skill screen, the abilities screen and Recruits' screens get the Slate background and border. */
    static void onTooltipColour(RenderTooltipEvent.Color event) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof SkillsScreen || screen instanceof AbilityScreen || screen != null && SlateRecruits.isRecruits(screen)) {
            event.setBackground(TOOLTIP);
            event.setBorderStart(TOOLTIP_BORDER);
            event.setBorderEnd(TOOLTIP_BORDER);
        }
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        box(graphics, x, y, width, height, PANEL, BORDER);
    }

    /** A panel with its title at the top left over a hairline rule; content starts 28px down. */
    static void panel(GuiGraphics graphics, Font font, Component title, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height);
        graphics.drawString(font, title, x + 10, y + 9, TEXT, false);
        rule(graphics, x + 10, x + width - 10, y + 21);
    }

    /** A hairline divider from x0 to x1. */
    static void rule(GuiGraphics graphics, int x0, int x1, int y) {
        graphics.fill(x0, y, x1, y + 1, BORDER);
    }

    static void slot(GuiGraphics graphics, int x, int y, int size, Slot state) {
        int fill = state == Slot.PICKED ? SLOT_PICKED : SLOT;
        int border = switch (state) {
            case EMPTY -> SLOT_EMPTY_BORDER;
            case FILLED -> SLOT_BORDER;
            case PICKED -> ACCENT;
        };
        box(graphics, x, y, size, size, fill, border);
    }

    static void button(GuiGraphics graphics, int x, int y, int width, int height, Button state) {
        switch (state) {
            case NORMAL -> box(graphics, x, y, width, height, SLOT, SLOT_BORDER);
            case HOVERED -> box(graphics, x, y, width, height, SLOT_HOVERED, ACCENT);
            case INACTIVE -> box(graphics, x, y, width, height, PANEL, SLOT_EMPTY_BORDER);
        }
    }

    /** A width x height bar on a dark track, filled from the left by fraction (0-1). */
    static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int colour) {
        graphics.fill(x, y, x + width, y + height, TRACK);
        graphics.fill(x, y, x + Math.round(width * fraction), y + height, colour);
    }

    /** A disc of the given radius with a 1px ring, drawn row by row. */
    static void disc(GuiGraphics graphics, int centerX, int centerY, int radius, boolean accent) {
        int ring = accent ? ACCENT : BORDER;
        for (int dy = -radius; dy < radius; dy++) {
            double row = dy + 0.5;
            int outer = (int) Math.round(Math.sqrt(radius * radius - row * row));
            int inner = Math.abs(row) < radius - 1 ? (int) Math.round(Math.sqrt((radius - 1) * (radius - 1) - row * row)) : 0;
            int y = centerY + dy;
            if (inner == 0) {
                graphics.fill(centerX - outer, y, centerX + outer, y + 1, ring);
                continue;
            }
            graphics.fill(centerX - outer, y, centerX - inner, y + 1, ring);
            graphics.fill(centerX - inner, y, centerX + inner, y + 1, PANEL);
            graphics.fill(centerX + inner, y, centerX + outer, y + 1, ring);
        }
    }

    /** A box with 2px-cut corners and a 1px border; the fills never overlap, so translucent colours stay even. */
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
