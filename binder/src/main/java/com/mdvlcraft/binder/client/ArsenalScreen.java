package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.BindArsenalPacket;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Shadow Arsenal (Formless Sword's Select mode): the absorbed weapons above, the sword wheel's 8 slots below. Click a
 * weapon, then a slot, to bind it there; right-click a slot to clear it. Laid out like client/AbilityScreen.
 */
public final class ArsenalScreen extends Screen {
    private static final int WIDTH = 194;
    private static final int HEIGHT = 172;   // fits a 180-high screen (720p at GUI scale 4)
    private static final int ROWS = 3;
    private Item picked;

    public ArsenalScreen() {
        super(Component.translatable("screen.mdvlcraft.arsenal"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return (this.height - HEIGHT) / 2;
    }

    private int itemX(int index) {
        return this.left() + 10 + index % 8 * 22;
    }

    private int itemY(int index) {
        return this.top() + 28 + index / 8 * 22;
    }

    private int slotX(int slot) {
        return this.left() + 10 + slot * 22;
    }

    private int slotY() {
        return this.top() + 104;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int textX = this.left() + 10;
        DqGui.panel(graphics, this.font, this.title, this.left(), this.top(), WIDTH, HEIGHT);
        List<Item> arsenal = ClientArsenal.arsenal();
        if (arsenal.isEmpty()) {
            List<FormattedCharSequence> none = this.font.split(Component.translatable("screen.mdvlcraft.arsenal.none"), 174);
            for (int line = 0; line < none.size(); line++) {
                graphics.drawString(this.font, none.get(line), textX, this.itemY(0) + 4 + line * 10, DqGui.MUTED, false);
            }
        }
        for (int i = 0; i < Math.min(ROWS * 8, arsenal.size()); i++) {
            DqGui.slot(graphics, this.itemX(i), this.itemY(i), 20, arsenal.get(i) == this.picked ? DqGui.Slot.PICKED : DqGui.Slot.FILLED);
            graphics.renderItem(new ItemStack(arsenal.get(i)), this.itemX(i) + 2, this.itemY(i) + 2);
        }
        graphics.drawString(this.font, Component.translatable("screen.mdvlcraft.arsenal.wheel"), textX, this.slotY() - 12, DqGui.MUTED, false);
        for (int slot = 0; slot < 8; slot++) {
            int x = this.slotX(slot);
            DqGui.slot(graphics, x, this.slotY(), 20, ClientArsenal.wheel(slot).isPresent() ? DqGui.Slot.FILLED : DqGui.Slot.EMPTY);
            ClientArsenal.wheel(slot).ifPresent(item -> graphics.renderItem(new ItemStack(item), x + 2, this.slotY() + 2));
            String number = String.valueOf(slot + 1);
            graphics.drawString(this.font, number, x + (20 - this.font.width(number)) / 2 + 1, this.slotY() + 23, DqGui.MUTED, false);
        }
        int hintTop = this.slotY() + 34;
        DqGui.rule(graphics, textX, this.left() + WIDTH - 10, hintTop - 6);
        Component hint = Component.translatable(this.picked == null ? "screen.mdvlcraft.arsenal.hint_pick" : "screen.mdvlcraft.arsenal.hint_place");
        List<FormattedCharSequence> lines = this.font.split(hint, 174);
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(this.font, lines.get(line), textX, hintTop + line * 10, DqGui.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.hovered(mouseX, mouseY).ifPresent(item -> graphics.renderTooltip(this.font, new ItemStack(item), mouseX, mouseY));
    }

    private Optional<Item> hovered(double mouseX, double mouseY) {
        List<Item> arsenal = ClientArsenal.arsenal();
        for (int i = 0; i < Math.min(ROWS * 8, arsenal.size()); i++) {
            if (inside(mouseX, mouseY, this.itemX(i), this.itemY(i))) {
                return Optional.of(arsenal.get(i));
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            if (inside(mouseX, mouseY, this.slotX(slot), this.slotY())) {
                return ClientArsenal.wheel(slot);
            }
        }
        return Optional.empty();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Item> arsenal = ClientArsenal.arsenal();
        for (int i = 0; i < Math.min(ROWS * 8, arsenal.size()); i++) {
            if (inside(mouseX, mouseY, this.itemX(i), this.itemY(i))) {
                this.picked = arsenal.get(i) == this.picked ? null : arsenal.get(i);
                return true;
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            if (inside(mouseX, mouseY, this.slotX(slot), this.slotY())) {
                if (button == 1) {
                    BinderNetwork.sendToServer(new BindArsenalPacket(slot, Optional.empty()));
                } else if (this.picked != null) {
                    BinderNetwork.sendToServer(new BindArsenalPacket(slot, Optional.of(ClientArsenal.id(this.picked))));
                    this.picked = null;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
