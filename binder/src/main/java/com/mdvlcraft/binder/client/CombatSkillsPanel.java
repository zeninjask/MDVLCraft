package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.epicskill.EquipSkillPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The abilities screen's second box: Epic Fight's skill slots. Click a slot, then one of the learned skills of its kind
 * to equip it; right-click a slot to empty it. The server checks every change (EpicSkillEquip).
 */
final class CombatSkillsPanel {
    static final int WIDTH = 150;
    private static final int ROW = 20;
    private static final int CELL = 20;
    private static final int COLUMNS = 7;
    /** Epic Fight's own learnable slots (the skill trees teach only these); add-ons' extra slots are theirs to fill. */
    private static final List<SkillSlot> SLOTS = Arrays.stream(SkillSlots.values())
            .filter(slot -> slot.category().learnable())
            .map(SkillSlot.class::cast)
            .toList();

    private final Font font;
    private int left;
    private int top;
    @Nullable
    private SkillSlot picked;

    CombatSkillsPanel(Font font) {
        this.font = font;
    }

    void place(int left, int top) {
        this.left = left;
        this.top = top;
    }

    private static CapabilitySkill skills() {
        return EpicFightCapabilities.getLocalPlayerPatch(Minecraft.getInstance().player).getSkillCapability();
    }

    private List<Skill> choices() {
        if (picked == null) {
            return List.of();
        }
        return skills().listAcquiredSkills().filter(skill -> skill.getCategory() == picked.category()).toList();
    }

    private List<FormattedCharSequence> hint() {
        return font.split(Component.translatable("screen.mdvlcraft.skills.hint"), WIDTH - 12);
    }

    private int slotY(int index) {
        return top + 22 + index * ROW;
    }

    private int choicesTop() {
        return slotY(SLOTS.size()) + 14;
    }

    private int choiceX(int index) {
        return left + 8 + (index % COLUMNS) * CELL;
    }

    private int choiceY(int index) {
        return choicesTop() + (index / COLUMNS) * CELL;
    }

    private int hintTop() {
        int rows = Math.max(1, (choices().size() + COLUMNS - 1) / COLUMNS);
        return choicesTop() + rows * CELL + 4;
    }

    int height() {
        return hintTop() + hint().size() * 10 + 4 - top;
    }

    void render(GuiGraphics graphics, int height) {
        MedievalGui.panel(graphics, left, top, WIDTH, height);
        MedievalGui.centeredInk(graphics, font, Component.translatable("screen.mdvlcraft.skills"), left + WIDTH / 2, top + 8, MedievalGui.INK);
        CapabilitySkill skills = skills();
        for (int i = 0; i < SLOTS.size(); i++) {
            SkillSlot slot = SLOTS.get(i);
            SkillContainer container = skills.getSkillContainerFor(slot);
            Skill skill = container.getSkill();
            int x = left + 8;
            int y = slotY(i);
            MedievalGui.frame(graphics, "task", slot == picked ? "affordable" : skill != null ? "unlocked" : "locked", x - 1, y - 1, 20);
            if (skill != null) {
                drawIcon(graphics, skill, x + 1, y + 1);
            }
            graphics.drawString(font, Component.literal(SkillSlot.ENUM_MANAGER.toTranslated(slot)), x + 22, y, MedievalGui.INK_FADED, false);
            Component name = skill != null ? Component.translatable(skill.getTranslationKey()) : Component.translatable("screen.mdvlcraft.skills.empty");
            graphics.drawString(font, name, x + 22, y + 10, skill != null ? MedievalGui.INK : MedievalGui.INK_FADED, false);
            if (container.onReplaceCooldown()) {
                String seconds = (container.getReplaceCooldown() + 19) / 20 + "s";
                graphics.drawString(font, seconds, left + WIDTH - 6 - font.width(seconds), y, 0xFF9A1E1E, false);
            }
        }
        graphics.drawString(font, picked == null ? Component.translatable("screen.mdvlcraft.skills.learned")
                : Component.translatable("screen.mdvlcraft.skills.learned_for", Component.literal(SkillSlot.ENUM_MANAGER.toTranslated(picked))),
                left + 8, choicesTop() - 11, MedievalGui.INK, false);
        List<Skill> choices = choices();
        if (picked != null && choices.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.mdvlcraft.skills.none"), left + 8, choicesTop() + 5, MedievalGui.INK_FADED, false);
        }
        for (int i = 0; i < choices.size(); i++) {
            Skill skill = choices.get(i);
            MedievalGui.frame(graphics, "task", skills.isEquipping(skill) ? "unlocked" : "available", choiceX(i) - 1, choiceY(i) - 1, 20);
            drawIcon(graphics, skill, choiceX(i) + 1, choiceY(i) + 1);
        }
        List<FormattedCharSequence> hint = hint();
        for (int line = 0; line < hint.size(); line++) {
            FormattedCharSequence text = hint.get(line);
            graphics.drawString(font, text, left + WIDTH / 2 - font.width(text) / 2, hintTop() + line * 10, MedievalGui.INK_FADED, false);
        }
    }

    private static void drawIcon(GuiGraphics graphics, Skill skill, int x, int y) {
        RenderSystem.enableBlend();
        graphics.blit(skill.getSkillTexture(), x, y, 16, 16, 0, 0, 128, 128, 128, 128);
    }

    Optional<List<FormattedCharSequence>> tooltip(double mouseX, double mouseY) {
        return hovered(mouseX, mouseY).map(this::tooltip);
    }

    private List<FormattedCharSequence> tooltip(Skill skill) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(skill.getTranslationKey()).withStyle(ChatFormatting.GOLD).getVisualOrderText());
        lines.addAll(font.split(Component.translatable(skill.getTranslationKey() + ".tooltip",
                skill.getTooltipArgsOfScreen(new ArrayList<>()).toArray()).withStyle(ChatFormatting.GRAY), 220));
        return lines;
    }

    private Optional<Skill> hovered(double mouseX, double mouseY) {
        for (int i = 0; i < SLOTS.size(); i++) {
            if (inside(mouseX, mouseY, left + 8, slotY(i))) {
                return Optional.ofNullable(skills().getSkillContainerFor(SLOTS.get(i)).getSkill());
            }
        }
        List<Skill> choices = choices();
        for (int i = 0; i < choices.size(); i++) {
            if (inside(mouseX, mouseY, choiceX(i), choiceY(i))) {
                return Optional.of(choices.get(i));
            }
        }
        return Optional.empty();
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < SLOTS.size(); i++) {
            SkillSlot slot = SLOTS.get(i);
            if (inside(mouseX, mouseY, left + 8, slotY(i))) {
                if (button == 1) {
                    BinderNetwork.sendToServer(new EquipSkillPacket(slot.universalOrdinal(), Optional.empty()));
                } else {
                    picked = slot == picked ? null : slot;
                }
                return true;
            }
        }
        List<Skill> choices = choices();
        for (int i = 0; i < choices.size(); i++) {
            if (inside(mouseX, mouseY, choiceX(i), choiceY(i))) {
                BinderNetwork.sendToServer(new EquipSkillPacket(picked.universalOrdinal(), Optional.of(choices.get(i).getRegistryName())));
                picked = null;
                return true;
            }
        }
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }
}
