package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.Loadout;
import com.mdvlcraft.binder.network.AssignSlotPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lists every unlocked spell and technique. Click an ability, then a wheel slot, to put it on the wheel;
 * right-click a slot to empty it.
 */
public final class AbilityScreen extends Screen {
    private static final int CELL = 20;
    private static final int COLUMNS = 8;
    private static final int PANEL_WIDTH = COLUMNS * CELL + 16;

    private Ability picked;

    public AbilityScreen() {
        super(Component.translatable("screen.mdvlcraft.abilities"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Ability> abilities() {
        return new ArrayList<>(ClientAbilities.granted().keySet());
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int listTop() {
        return height / 2 - 70;
    }

    private int abilityX(int index) {
        return left() + 8 + (index % COLUMNS) * CELL;
    }

    private int abilityY(int index) {
        return listTop() + (index / COLUMNS) * CELL;
    }

    private int slotX(int slot) {
        return left() + 8 + slot * CELL;
    }

    private int slotY() {
        return height / 2 + 60;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Component hint = Component.translatable(picked == null ? "screen.mdvlcraft.abilities.hint_pick" : "screen.mdvlcraft.abilities.hint_place");
        List<FormattedCharSequence> hintLines = font.split(hint, PANEL_WIDTH - 12);
        MedievalGui.panel(graphics, left(), listTop() - 30, PANEL_WIDTH, slotY() + 36 + hintLines.size() * 10 - (listTop() - 30));
        MedievalGui.centeredInk(graphics, font, title, width / 2, listTop() - 22, MedievalGui.INK);
        List<Ability> abilities = abilities();
        if (abilities.isEmpty()) {
            MedievalGui.centeredInk(graphics, font, Component.translatable("screen.mdvlcraft.abilities.none"), width / 2, listTop() + 20, MedievalGui.INK_FADED);
        }
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            int x = abilityX(i);
            int y = abilityY(i);
            MedievalGui.frame(graphics, "task", ability == picked ? "affordable" : "available", x - 1, y - 1, 20);
            AbilityDisplay.drawIcon(graphics, ability, x + 1, y + 1);
        }
        graphics.drawString(font, Component.translatable("screen.mdvlcraft.abilities.wheel"), left() + 8, slotY() - 12, MedievalGui.INK, false);
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            int x = slotX(slot);
            MedievalGui.frame(graphics, "task", ClientAbilities.slot(slot).isPresent() ? "unlocked" : "locked", x - 1, slotY() - 1, 20);
            ClientAbilities.slot(slot).ifPresent(ability -> AbilityDisplay.drawIcon(graphics, ability, x + 1, slotY() + 1));
            graphics.drawString(font, String.valueOf(slot + 1), x + 7, slotY() + 20, MedievalGui.INK_FADED, false);
        }
        for (int line = 0; line < hintLines.size(); line++) {
            FormattedCharSequence text = hintLines.get(line);
            graphics.drawString(font, text, width / 2 - font.width(text) / 2, slotY() + 32 + line * 10, MedievalGui.INK_FADED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        hovered(mouseX, mouseY).ifPresent(ability -> graphics.renderComponentTooltip(font, AbilityDisplay.tooltip(ability, ClientAbilities.level(ability)), mouseX, mouseY));
    }

    private Optional<Ability> hovered(double mouseX, double mouseY) {
        List<Ability> abilities = abilities();
        for (int i = 0; i < abilities.size(); i++) {
            if (inside(mouseX, mouseY, abilityX(i), abilityY(i))) {
                return Optional.of(abilities.get(i));
            }
        }
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            if (inside(mouseX, mouseY, slotX(slot), slotY())) {
                return ClientAbilities.slot(slot).filter(ability -> ClientAbilities.level(ability) > 0);
            }
        }
        return Optional.empty();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Ability> abilities = abilities();
        for (int i = 0; i < abilities.size(); i++) {
            if (inside(mouseX, mouseY, abilityX(i), abilityY(i))) {
                picked = abilities.get(i) == picked ? null : abilities.get(i);
                return true;
            }
        }
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            if (inside(mouseX, mouseY, slotX(slot), slotY())) {
                if (button == 1) {
                    BinderNetwork.sendToServer(new AssignSlotPacket(slot, Optional.empty()));
                } else if (picked != null) {
                    BinderNetwork.sendToServer(new AssignSlotPacket(slot, Optional.of(picked.id())));
                    picked = null;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.OPEN_ABILITIES.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
