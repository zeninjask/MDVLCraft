package com.mdvlcraft.binder.client.picker;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.PickerOpenPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * A full-screen picker in the style of Class Enhancement's class selection: a scrolling list of choices on the left (with
 * headings between groups), the hovered or selected choice's name, icon and description in a panel on the right beside
 * the player, and a confirm button that must be held for a second. Used for the vocation (VocationPicker) and for every
 * Origins layer: origin, Martial class and Job (OriginPicker).
 */
public class PickerScreen extends Screen {
    private static final int INK = 0xF0050505;
    private static final int SURFACE = 0xFF0D0D1A;
    private static final int SURFACE_HI = 0xFF151528;
    private static final int LINE = 0xFF262640;
    private static final int LINE_HI = 0xFF3C3C62;
    private static final int TEXT_HI = 0xEDEDF7;
    private static final int TEXT_BODY = 0xB4B3D0;
    private static final int TEXT_DIM = 0x6E6C94;
    private static final int ACCENT = 0x9AA3C4;
    private static final int ROW = 22;
    private static final int MAX_LIST_W = 170;
    private static final int MAX_PANEL_W = 220;
    private static final long HOLD_MS = 1000L;

    /** A row of the list: a heading (option == null) or a choice. */
    public record Entry(Component name, Component description, List<Detail> details, ItemStack icon, String id) {
        public static Entry heading(Component name) {
            return new Entry(name, Component.empty(), List.of(), ItemStack.EMPTY, null);
        }

        boolean isHeading() {
            return id == null;
        }
    }

    /** A skill, spell or ability listed under the description; hovering its name shows its description. */
    public record Detail(Component name, Component description) {
    }

    private final List<Entry> entries;
    private final Consumer<Entry> onConfirm;
    private final boolean closable;
    private Entry selected;
    private float scroll;
    private long holdStart = -1L;
    private int listW;
    private int panelW;
    private int listLeft;
    private int listTop;
    private int listBottom;
    private int panelLeft;
    private int confirmX;
    private int confirmY;

    public PickerScreen(Component title, List<Entry> entries, boolean closable, Consumer<Entry> onConfirm) {
        super(title);
        this.entries = entries;
        this.closable = closable;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        // narrow screens (high gui scale) shrink both columns so neither runs off the edge
        int gap = Math.min(40, this.width / 20);
        int room = this.width - 16 - gap;
        this.listW = Math.min(MAX_LIST_W, room * 2 / 5);
        this.panelW = Math.min(MAX_PANEL_W, room - this.listW);
        this.listLeft = (this.width - this.listW - gap - this.panelW) / 2;
        this.panelLeft = this.listLeft + this.listW + gap;
        this.listTop = 32;
        this.listBottom = this.height - 16;
        this.confirmX = this.panelLeft + (this.panelW - 120) / 2;
        this.confirmY = this.listBottom - 22;
        this.scroll = Mth.clamp(this.scroll, 0.0F, maxScroll());
        BinderNetwork.sendToServer(new PickerOpenPacket(true));   // invulnerable while picking (skills/PickerShield)
    }

    @Override
    public void removed() {
        BinderNetwork.sendToServer(new PickerOpenPacket(false));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.closable;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float maxScroll() {
        return Math.max(0, this.entries.size() * ROW - (this.listBottom - this.listTop));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, INK);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, TEXT_HI);

        g.enableScissor(this.listLeft, this.listTop, this.listLeft + this.listW, this.listBottom);
        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            int y = this.listTop + i * ROW - (int)this.scroll;
            if (y + ROW < this.listTop || y > this.listBottom) {
                continue;
            }
            if (entry.isHeading()) {
                g.drawString(this.font, entry.name(), this.listLeft + 4, y + 9, ACCENT, false);
                g.fill(this.listLeft + 4, y + 19, this.listLeft + this.listW - 4, y + 20, LINE);
                continue;
            }
            boolean hovered = mouseX >= this.listLeft && mouseX < this.listLeft + this.listW && mouseY >= y && mouseY < y + ROW - 2
                && mouseY >= this.listTop && mouseY < this.listBottom;
            boolean isSelected = entry == this.selected;
            g.fill(this.listLeft, y, this.listLeft + this.listW, y + ROW - 2, isSelected ? SURFACE_HI : SURFACE);
            g.renderOutline(this.listLeft, y, this.listW, ROW - 2, isSelected || hovered ? LINE_HI : LINE);
            if (!entry.icon().isEmpty()) {
                g.renderItem(entry.icon(), this.listLeft + 3, y + 2);
            }
            g.drawString(this.font, entry.name(), this.listLeft + 24, y + 6, isSelected ? TEXT_HI : TEXT_BODY, false);
        }
        g.disableScissor();

        Entry shown = this.selected;
        if (shown != null) {
            renderPanel(g, shown, mouseX, mouseY);
        } else {
            g.drawCenteredString(this.font, Component.translatable("gui.mdvlcraft.picker.choose"), this.panelLeft + this.panelW / 2, this.height / 2, TEXT_DIM);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPanel(GuiGraphics g, Entry entry, int mouseX, int mouseY) {
        g.fill(this.panelLeft, this.listTop, this.panelLeft + this.panelW, this.listBottom, SURFACE);
        g.renderOutline(this.panelLeft, this.listTop, this.panelW, this.listBottom - this.listTop, LINE);
        if (!entry.icon().isEmpty()) {
            g.renderItem(entry.icon(), this.panelLeft + 8, this.listTop + 8);
        }
        g.drawString(this.font, entry.name(), this.panelLeft + 30, this.listTop + 12, TEXT_HI, false);
        g.fill(this.panelLeft + 8, this.listTop + 28, this.panelLeft + this.panelW - 8, this.listTop + 29, LINE);
        int y = this.listTop + 34;
        for (FormattedCharSequence line : this.font.split(entry.description(), this.panelW - 16)) {
            if (y + 10 > this.confirmY - 4) {
                break;
            }
            g.drawString(this.font, line, this.panelLeft + 8, y, TEXT_BODY, false);
            y += 10;
        }
        Detail hovered = this.renderDetails(g, entry.details(), y + 4, mouseX, mouseY);

        float progress = this.holdStart < 0 ? 0.0F : Math.min(1.0F, (Util.getMillis() - this.holdStart) / (float)HOLD_MS);
        boolean overConfirm = overConfirm(mouseX, mouseY);
        g.fill(this.confirmX, this.confirmY, this.confirmX + 120, this.confirmY + 18, SURFACE_HI);
        g.fill(this.confirmX, this.confirmY, this.confirmX + (int)(120 * progress), this.confirmY + 18, 0xFF2E3A66);
        g.renderOutline(this.confirmX, this.confirmY, 120, 18, overConfirm ? LINE_HI : LINE);
        g.drawCenteredString(this.font, Component.translatable("gui.mdvlcraft.picker.confirm"), this.confirmX + 60, this.confirmY + 5, TEXT_HI);
        if (hovered != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(hovered.name().copy().withStyle(ChatFormatting.BOLD).getVisualOrderText());
            lines.addAll(this.font.split(hovered.description(), 200));
            g.renderTooltip(this.font, lines, mouseX, mouseY);
        }
        if (progress >= 1.0F) {
            this.holdStart = -1L;
            confirm(entry);
        }
    }

    /** The details' names after an "Abilities:" heading, flowed into lines like words; returns the one under the mouse. */
    private Detail renderDetails(GuiGraphics g, List<Detail> details, int top, int mouseX, int mouseY) {
        int left = this.panelLeft + 8;
        int right = this.panelLeft + this.panelW - 8;
        int bottom = this.confirmY - 4;
        if (details.isEmpty() || top + 20 > bottom) {
            return null;
        }
        g.drawString(this.font, Component.translatable("gui.mdvlcraft.picker.abilities"), left, top, ACCENT, false);
        int x = left;
        int y = top + 10;
        Detail hovered = null;
        for (int i = 0; i < details.size(); i++) {
            Detail detail = details.get(i);
            int width = this.font.width(detail.name());
            if (x > left && x + width > right) {
                x = left;
                y += 10;
            }
            if (y + 10 > bottom) {
                break;
            }
            boolean over = mouseX >= x && mouseX < x + width && mouseY >= y - 1 && mouseY < y + 9;
            g.drawString(this.font, detail.name().copy().withStyle(over ? ChatFormatting.UNDERLINE : ChatFormatting.RESET), x, y, over ? TEXT_HI : TEXT_BODY, false);
            if (over) {
                hovered = detail;
            }
            x += width;
            if (i < details.size() - 1) {
                g.drawString(this.font, ",", x, y, TEXT_DIM, false);
                x += this.font.width(", ");
            }
        }
        return hovered;
    }

    private boolean overConfirm(double mouseX, double mouseY) {
        return mouseX >= this.confirmX && mouseX < this.confirmX + 120 && mouseY >= this.confirmY && mouseY < this.confirmY + 18;
    }

    private void confirm(Entry entry) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
        this.onConfirm.accept(entry);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.selected != null && overConfirm(mouseX, mouseY)) {
            this.holdStart = Util.getMillis();
            return true;
        }
        if (button == 0 && mouseX >= this.listLeft && mouseX < this.listLeft + this.listW && mouseY >= this.listTop && mouseY < this.listBottom) {
            int index = (int)((mouseY - this.listTop + this.scroll) / ROW);
            if (index >= 0 && index < this.entries.size() && !this.entries.get(index).isHeading() && this.entries.get(index) != this.selected) {
                this.selected = this.entries.get(index);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.holdStart = -1L;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scroll = Mth.clamp(this.scroll - (float)delta * ROW, 0.0F, maxScroll());
        return true;
    }
}
