package com.mdvlcraft.binder.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;

/**
 * The Dragon Quest IX look for every menu: windows with a rounded three-pixel bevel (light grey, white, dark grey) on
 * black or charcoal, and the thick blue frame that marks what is selected. The frame corners are copied pixel for pixel
 * from the game's menus. Vanilla and Forge buttons and sliders draw with it too (mixin/AbstractButtonMixin,
 * AbstractSliderButtonMixin). The ability HUD keeps its own look (SlateGui).
 */
public final class DqGui {
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA5A5A5;
    /** Headings and names: the pale blue of the game's highlighted text. */
    public static final int HEADING = 0xFF9CC6F7;
    public static final int BLACK = 0xFF000000;
    public static final int CHARCOAL = 0xFF313131;
    public static final int BLUE = 0xFF1863C6;
    public static final int RULE = 0xFFFFFFFF;
    public static final int RULE_FAINT = 0xFF424242;
    private static final int TOOLTIP_BACKGROUND = 0xF0000000;
    private static final int TOOLTIP_BORDER_TOP = 0xFFFFFFFF;
    private static final int TOOLTIP_BORDER_BOTTOM = 0xFFA5A5A5;
    /** Corner pixels of a window; 'F' is the fill and '.' is left alone. Edges continue the outer three rows/columns. */
    private static final String[] WINDOW_TOP_LEFT = {"...ABB", ".ACDDD", ".CDDAE", "ADDEFF", "BDAFFF", "BDEFFF"};
    private static final String[] WINDOW_TOP_RIGHT = {"BBA...", "DDDCA.", "EADDC.", "FFEDDE", "FFFBDE", "FFFBDE"};
    private static final String[] WINDOW_BOTTOM_LEFT = {"BDEFFF", "BDAFFF", "ADDEFF", "BCDDBB", ".ACDDD", "...EEE"};
    private static final String[] WINDOW_BOTTOM_RIGHT = {"FFFBDE", "FFFBDE", "FFADDE", "BBDDAE", "DDDAE.", "EEE..."};
    private static final Map<Character, Integer> WINDOW_COLOURS = Map.of(
        'A', 0xFF848484, 'B', 0xFFA5A5A5, 'C', 0xFFD6D6D6, 'D', 0xFFFFFFFF, 'E', 0xFF424242
    );
    private static final Map<Character, Integer> DIM_COLOURS = Map.of(
        'A', 0xFF4A4A4A, 'B', 0xFF5A5A5A, 'C', 0xFF6B6B6B, 'D', 0xFF7B7B7B, 'E', 0xFF2A2A2A
    );
    /** Top-left corner of the selected frame; the other corners mirror it. */
    private static final String[] SELECTED_CORNER = {"...HHHHH", "..HIIIII", ".HIJJJJJ", "HIJJJJJJ", "HIJJJJJJ", "HIJJJJJJ", "HIJJJJJG", "HIJJJJGG"};
    private static final Map<Character, Integer> SELECTED_COLOURS = Map.of('H', 0xFF8C8484, 'I', 0xFFE7DEDE, 'J', BLUE);
    private static final int WINDOW_EDGE = 3;
    private static final int SELECTED_EDGE = 6;

    private DqGui() {
    }

    public static void onTooltipColour(RenderTooltipEvent.Color event) {
        event.setBackground(TOOLTIP_BACKGROUND);
        event.setBorderStart(TOOLTIP_BORDER_TOP);
        event.setBorderEnd(TOOLTIP_BORDER_BOTTOM);
    }

    /** A black window, the game's menus and message boxes. */
    public static void window(GuiGraphics g, int x, int y, int w, int h) {
        frame(g, x, y, w, h, BLACK, WINDOW_COLOURS);
    }

    /** A black window with its title centred above a white rule. */
    public static void window(GuiGraphics g, Font font, Component title, int x, int y, int w, int h) {
        window(g, x, y, w, h);
        g.drawCenteredString(font, title, x + w / 2, y + 8, TEXT);
        rule(g, x + WINDOW_EDGE, x + w - WINDOW_EDGE, y + 20);
    }

    /** A charcoal window, the game's lists and status panels. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        frame(g, x, y, w, h, CHARCOAL, WINDOW_COLOURS);
    }

    /** A charcoal window with its title centred above a white rule. */
    public static void panel(GuiGraphics g, Font font, Component title, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
        g.drawCenteredString(font, title, x + w / 2, y + 8, TEXT);
        rule(g, x + WINDOW_EDGE, x + w - WINDOW_EDGE, y + 20);
    }

    /** The thick blue frame around whatever is selected. */
    public static void selected(GuiGraphics g, int x, int y, int w, int h, int fill) {
        require(w, h, 2 * SELECTED_CORNER.length);
        g.fill(x + SELECTED_EDGE, y + SELECTED_EDGE, x + w - SELECTED_EDGE, y + h - SELECTED_EDGE, fill);
        int[] bands = {SELECTED_COLOURS.get('H'), SELECTED_COLOURS.get('I'), BLUE, BLUE, BLUE, BLUE};
        int corner = SELECTED_CORNER.length;
        for (int i = 0; i < SELECTED_EDGE; i++) {
            g.fill(x + corner, y + i, x + w - corner, y + i + 1, bands[i]);
            g.fill(x + corner, y + h - 1 - i, x + w - corner, y + h - i, bands[i]);
            g.fill(x + i, y + corner, x + i + 1, y + h - corner, bands[i]);
            g.fill(x + w - 1 - i, y + corner, x + w - i, y + h - corner, bands[i]);
        }

        for (int row = 0; row < corner; row++) {
            String line = SELECTED_CORNER[row];
            String mirrored = new StringBuilder(line).reverse().toString();
            pattern(g, x, y + row, line, fill, SELECTED_COLOURS, 'G');
            pattern(g, x + w - corner, y + row, mirrored, fill, SELECTED_COLOURS, 'G');
            pattern(g, x, y + h - 1 - row, line, fill, SELECTED_COLOURS, 'G');
            pattern(g, x + w - corner, y + h - 1 - row, mirrored, fill, SELECTED_COLOURS, 'G');
        }
    }

    /** Any size: other mods' buttons can be smaller than a window, and those get a thin frame. */
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active) {
        int corner = 2 * WINDOW_TOP_LEFT.length;
        if (w < corner || h < corner) {
            thin(g, x, y, w, h, !active ? CHARCOAL : (hovered ? BLUE : BLACK), active ? RULE : DIM_COLOURS.get('D'));
        } else if (!active) {
            frame(g, x, y, w, h, CHARCOAL, DIM_COLOURS);
        } else if (hovered && w >= 2 * SELECTED_CORNER.length && h >= 2 * SELECTED_CORNER.length) {
            selected(g, x, y, w, h, BLACK);
        } else if (hovered) {
            // Too small for the thick frame (the 14 px back buttons): a window with a blue face instead.
            frame(g, x, y, w, h, BLUE, WINDOW_COLOURS);
        } else {
            window(g, x, y, w, h);
        }
    }

    /** A slider's handle: a white-edged blue key, 8 px wide like vanilla's. */
    public static void handle(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF424242);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF424242);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, TEXT);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, hovered ? 0xFF5A9CF7 : BLUE);
    }

    /** A thin scroll bar for a list showing `visible` of `total` rows from row `first`; nothing when everything fits. */
    public static void scrollbar(GuiGraphics g, int x, int y, int h, int first, int visible, int total) {
        if (total <= visible) {
            return;
        }
        g.fill(x, y, x + 2, y + h, RULE_FAINT);
        int thumb = Math.max(6, h * visible / total);
        int thumbY = y + (h - thumb) * first / (total - visible);
        g.fill(x, thumbY, x + 2, thumbY + thumb, MUTED);
    }

    public static void rule(GuiGraphics g, int x0, int x1, int y) {
        g.fill(x0, y, x1, y + 1, RULE);
    }

    /** An icon slot: a thin white frame on charcoal, dark when empty, white and blue when picked. */
    public static void slot(GuiGraphics g, int x, int y, int size, Slot state) {
        switch (state) {
            case EMPTY -> thin(g, x, y, size, size, BLACK, RULE_FAINT);
            case FILLED -> thin(g, x, y, size, size, CHARCOAL, RULE);
            case PICKED -> {
                thin(g, x, y, size, size, BLUE, SELECTED_COLOURS.get('I'));
                g.fill(x + 2, y + 2, x + size - 2, y + size - 2, CHARCOAL);
            }
        }
    }

    /** A filled circle with a ring: white on black, or blue for the centre of the ability wheel. */
    public static void disc(GuiGraphics g, int centerX, int centerY, int radius, boolean accent) {
        int ring = accent ? BLUE : RULE;
        for (int dy = -radius; dy < radius; dy++) {
            double row = dy + 0.5;
            int outer = (int)Math.round(Math.sqrt(radius * radius - row * row));
            int inner = Math.abs(row) < radius - 2 ? (int)Math.round(Math.sqrt((radius - 2) * (radius - 2) - row * row)) : 0;
            int y = centerY + dy;
            if (inner == 0) {
                g.fill(centerX - outer, y, centerX + outer, y + 1, ring);
            } else {
                g.fill(centerX - outer, y, centerX - inner, y + 1, ring);
                g.fill(centerX - inner, y, centerX + inner, y + 1, BLACK);
                g.fill(centerX + inner, y, centerX + outer, y + 1, ring);
            }
        }
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int fill, Map<Character, Integer> colours) {
        int corner = WINDOW_TOP_LEFT.length;
        require(w, h, 2 * corner);
        g.fill(x + WINDOW_EDGE, y + WINDOW_EDGE, x + w - WINDOW_EDGE, y + h - WINDOW_EDGE, fill);
        // Outermost first: light grey, white, dark grey on the top and left; reversed on the bottom and right.
        char[] lit = {'B', 'D', 'E'};
        for (int i = 0; i < WINDOW_EDGE; i++) {
            int topLeft = colours.get(lit[i]);
            int bottomRight = colours.get(lit[WINDOW_EDGE - 1 - i]);
            g.fill(x + corner, y + i, x + w - corner, y + i + 1, topLeft);
            g.fill(x + i, y + corner, x + i + 1, y + h - corner, topLeft);
            g.fill(x + corner, y + h - 1 - i, x + w - corner, y + h - i, bottomRight);
            g.fill(x + w - 1 - i, y + corner, x + w - i, y + h - corner, bottomRight);
        }

        for (int row = 0; row < corner; row++) {
            pattern(g, x, y + row, WINDOW_TOP_LEFT[row], fill, colours, 'F');
            pattern(g, x + w - corner, y + row, WINDOW_TOP_RIGHT[row], fill, colours, 'F');
            pattern(g, x, y + h - corner + row, WINDOW_BOTTOM_LEFT[row], fill, colours, 'F');
            pattern(g, x + w - corner, y + h - corner + row, WINDOW_BOTTOM_RIGHT[row], fill, colours, 'F');
        }
    }

    /** One row of a corner, drawn as runs of equal colour. */
    private static void pattern(GuiGraphics g, int x, int y, String row, int fill, Map<Character, Integer> colours, char fillKey) {
        int start = 0;
        while (start < row.length()) {
            char key = row.charAt(start);
            int end = start + 1;
            while (end < row.length() && row.charAt(end) == key) {
                end++;
            }

            if (key != '.') {
                g.fill(x + start, y, x + end, y + 1, key == fillKey ? fill : colours.get(key));
            }

            start = end;
        }
    }

    private static void thin(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x + 1, y, x + w - 1, y + 1, border);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void require(int w, int h, int min) {
        if (w < min || h < min) {
            throw new IllegalArgumentException("A DQ frame needs at least " + min + " x " + min + " px, got " + w + " x " + h);
        }
    }

    public enum Slot {
        EMPTY,
        FILLED,
        PICKED;
    }

    /** A button in the DQ style for the binder's own screens. */
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
            DqGui.button(g, this.getX(), this.getY(), this.width, this.height, hot, this.active);
            g.drawCenteredString(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.width / 2 + this.labelOffset,
                this.getY() + (this.height - 8) / 2,
                this.active ? TEXT : MUTED
            );
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
