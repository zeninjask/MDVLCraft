package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.Loadout;
import com.mdvlcraft.binder.ability.SpellAbility;
import com.mdvlcraft.binder.network.AssignSlotPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lists every unlocked spell and technique. Click an ability, then a wheel slot, to put it on the wheel;
 * right-click a slot to empty it. Beside it, in its own box, are the Epic Fight skill slots (CombatSkillsPanel).
 */
public final class AbilityScreen extends Screen {
    static final int PAD = 10;
    static final int SLOT = 20;
    static final int CELL = SLOT + 2;
    private static final int COLUMNS = 8;
    /** Rows kept for unlocked abilities: two archetypes grant at most 19. */
    private static final int LIST_ROWS = 3;
    private static final int PANEL_WIDTH = 2 * PAD + COLUMNS * CELL - 2;
    private static final int GAP = 12;
    /** Layout down the box, from its top edge: ability list, wheel slots (with their numbers below), hint. */
    private static final int LIST_OFFSET = 28;
    private static final int SLOT_OFFSET = LIST_OFFSET + LIST_ROWS * CELL + 18;
    private static final int HINT_OFFSET = SLOT_OFFSET + SLOT + 20;

    private Ability picked;
    private CombatSkillsPanel combatSkills;

    public AbilityScreen() {
        super(Component.translatable("screen.mdvlcraft.abilities"));
    }

    @Override
    protected void init() {
        combatSkills = new CombatSkillsPanel(font);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Ability> abilities() {
        return new ArrayList<>(ClientAbilities.granted().keySet());
    }

    private int left() {
        return (width - PANEL_WIDTH - GAP - CombatSkillsPanel.WIDTH) / 2;
    }

    /** Both boxes share one height, centred on the screen. */
    private int panelHeight() {
        int hintLines = font.split(Component.translatable("screen.mdvlcraft.abilities.hint_pick"), PANEL_WIDTH - 2 * PAD).size();
        return Math.max(HINT_OFFSET + hintLines * 10 + 6, combatSkills.height());
    }

    private int top() {
        return (height - panelHeight()) / 2;
    }

    private int hintTop() {
        return top() + HINT_OFFSET;
    }

    private void placeCombatSkills() {
        combatSkills.place(left() + PANEL_WIDTH + GAP, top());
    }

    private int listTop() {
        return top() + LIST_OFFSET;
    }

    private int abilityX(int index) {
        return left() + PAD + (index % COLUMNS) * CELL;
    }

    private int abilityY(int index) {
        return listTop() + (index / COLUMNS) * CELL;
    }

    private int slotX(int slot) {
        return left() + PAD + slot * CELL;
    }

    private int slotY() {
        return top() + SLOT_OFFSET;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Component hint = Component.translatable(picked == null ? "screen.mdvlcraft.abilities.hint_pick" : "screen.mdvlcraft.abilities.hint_place");
        List<FormattedCharSequence> hintLines = font.split(hint, PANEL_WIDTH - 2 * PAD);
        placeCombatSkills();
        int panelHeight = panelHeight();
        int textX = left() + PAD;
        SlateGui.panel(graphics, font, title, left(), top(), PANEL_WIDTH, panelHeight);
        List<Ability> abilities = abilities();
        if (abilities.isEmpty()) {
            List<FormattedCharSequence> none = font.split(Component.translatable("screen.mdvlcraft.abilities.none"), PANEL_WIDTH - 2 * PAD);
            for (int line = 0; line < none.size(); line++) {
                graphics.drawString(font, none.get(line), textX, listTop() + 4 + line * 10, SlateGui.MUTED, false);
            }
        }
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            int x = abilityX(i);
            int y = abilityY(i);
            SlateGui.slot(graphics, x, y, SLOT, ability == picked ? SlateGui.Slot.PICKED : SlateGui.Slot.FILLED);
            AbilityDisplay.drawIcon(graphics, ability, x + 2, y + 2);
        }
        graphics.drawString(font, Component.translatable("screen.mdvlcraft.abilities.wheel"), textX, slotY() - 12, SlateGui.MUTED, false);
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            int x = slotX(slot);
            SlateGui.slot(graphics, x, slotY(), SLOT, ClientAbilities.slot(slot).isPresent() ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY);
            ClientAbilities.slot(slot).ifPresent(ability -> AbilityDisplay.drawIcon(graphics, ability, x + 2, slotY() + 2));
            String number = String.valueOf(slot + 1);
            graphics.drawString(font, number, x + (SLOT - font.width(number)) / 2 + 1, slotY() + SLOT + 3, SlateGui.MUTED, false);
        }
        SlateGui.rule(graphics, textX, left() + PANEL_WIDTH - PAD, hintTop() - 6);
        for (int line = 0; line < hintLines.size(); line++) {
            graphics.drawString(font, hintLines.get(line), textX, hintTop() + line * 10, SlateGui.MUTED, false);
        }
        combatSkills.render(graphics, panelHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
        hovered(mouseX, mouseY).ifPresent(ability -> {
            List<Component> lines = AbilityDisplay.tooltip(ability, ClientAbilities.level(ability));
            if (ability instanceof SpellAbility spell) {
                SpellPreviews.hover(spell.spell().getSpellResource(), ClientAbilities.level(ability));
                lines.add(1, SpellPreviews.prompt().copy().withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        });
        combatSkills.tooltip(mouseX, mouseY).ifPresent(lines -> graphics.renderTooltip(font, lines, mouseX, mouseY));
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
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
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
        placeCombatSkills();
        if (combatSkills.mouseClicked(mouseX, mouseY, button)) {
            return true;
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
