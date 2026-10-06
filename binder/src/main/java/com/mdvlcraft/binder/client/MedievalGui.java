package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Medieval GUI pieces shared by the ability screens: the same parchment, oak, gold and frames as the skill screen. */
final class MedievalGui {
    /** Dark ink for text on parchment. */
    static final int INK = 0xFF3B2414;
    static final int INK_FADED = 0xFF7A5A3A;

    private static final ResourceLocation PARCHMENT = texture("parchment");
    private static final int OUTLINE = 0xFF0C0805;
    private static final int WOOD = 0xFF4A321E;
    private static final int GOLD = 0xFFD6AA42;

    private MedievalGui() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/skills/" + name + ".png");
    }

    /** Parchment panel in an oak frame with gold trim; (x, y, width, height) is the parchment area. */
    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 5, y - 5, x + width + 5, y + height + 5, OUTLINE);
        graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, WOOD);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, GOLD);
        graphics.blit(PARCHMENT, x, y, 0, 0, width, height, 64, 64);
    }

    /**
     * One of the skill screen's frames, scaled to size. kind: minor, task, goal, challenge;
     * state: locked, available, affordable, unlocked, excluded.
     */
    static void frame(GuiGraphics graphics, String kind, String state, int x, int y, int size) {
        graphics.blit(texture("frame_" + kind + "_" + state), x, y, size, size, 0, 0, 26, 26, 26, 26);
    }

    static void centeredInk(GuiGraphics graphics, Font font, Component text, int centerX, int y, int colour) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, colour, false);
    }
}
