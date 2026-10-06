package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.Loadout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Radial ability wheel: shown while the wheel key is held; releasing it (or left-clicking) selects the slot under the mouse. */
public final class WheelScreen extends Screen {
    private static final int RADIUS = 56;
    private static final int SLOT = 24;
    private static final int DEAD_ZONE = 14;

    public WheelScreen() {
        super(Component.translatable("screen.mdvlcraft.wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int slotCenterX(int slot) {
        return width / 2 + (int) Math.round(RADIUS * Math.cos(angle(slot)));
    }

    private int slotCenterY(int slot) {
        return height / 2 + (int) Math.round(RADIUS * Math.sin(angle(slot)));
    }

    private static double angle(int slot) {
        return -Math.PI / 2 + slot * 2 * Math.PI / Loadout.SLOTS;
    }

    /** Slot whose direction from the centre is closest to the mouse, or -1 inside the dead zone. */
    private int hovered(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
            return -1;
        }
        double turn = (Math.atan2(dy, dx) + Math.PI / 2) / (2 * Math.PI);
        return Math.floorMod((int) Math.round(turn * Loadout.SLOTS), Loadout.SLOTS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hovered = hovered(mouseX, mouseY);
        int shown = hovered >= 0 ? hovered : ClientAbilities.selected();
        SlateGui.disc(graphics, width / 2, height / 2, RADIUS + SLOT / 2 + 8, false);
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            int x = slotCenterX(slot) - SLOT / 2;
            int y = slotCenterY(slot) - SLOT / 2;
            SlateGui.slot(graphics, x, y, SLOT, slot == shown ? SlateGui.Slot.PICKED
                    : ClientAbilities.slot(slot).isPresent() ? SlateGui.Slot.FILLED : SlateGui.Slot.EMPTY);
            ClientAbilities.slot(slot).ifPresent(ability -> drawAbility(graphics, ability, x + (SLOT - 16) / 2, y + (SLOT - 16) / 2));
        }
        SlateGui.disc(graphics, width / 2, height / 2, 18, true);
        ClientAbilities.slot(shown).ifPresent(ability -> {
            drawAbility(graphics, ability, width / 2 - 8, height / 2 - 8);
            int textWidth = font.width(ability.displayName());
            int bannerTop = height / 2 + RADIUS + SLOT / 2 + 14;
            SlateGui.panel(graphics, width / 2 - textWidth / 2 - 6, bannerTop, textWidth + 12, 16);
            graphics.drawString(font, ability.displayName(), width / 2 - textWidth / 2, bannerTop + 4, SlateGui.TEXT, false);
        });
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawAbility(GuiGraphics graphics, Ability ability, int x, int y) {
        AbilityDisplay.drawIcon(graphics, ability, x, y);
        if (ClientAbilities.level(ability) == 0) {
            graphics.fill(x, y, x + 16, y + 16, SlateGui.UNAVAILABLE);
        }
    }

    private void choose(double mouseX, double mouseY) {
        int hovered = hovered(mouseX, mouseY);
        if (hovered >= 0) {
            ClientAbilities.select(hovered);
        }
        onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.WHEEL.matches(keyCode, scanCode)) {
            double scale = (double) minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
            choose(minecraft.mouseHandler.xpos() * scale, minecraft.mouseHandler.ypos() * scale);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** Left-click selects the slot under the mouse; right-click opens the abilities screen to edit the wheel. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            minecraft.setScreen(new AbilityScreen());
        } else {
            choose(mouseX, mouseY);
        }
        return true;
    }
}
