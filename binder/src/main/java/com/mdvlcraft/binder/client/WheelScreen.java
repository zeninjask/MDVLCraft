package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Loadout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Radial ability wheel: shown while the wheel key is held; releasing it (or left-clicking) selects the slot under the mouse. */
public final class WheelScreen extends Screen {
    private static final int RADIUS = 56;
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
        for (int slot = 0; slot < Loadout.SLOTS; slot++) {
            int x = slotCenterX(slot) - 11;
            int y = slotCenterY(slot) - 11;
            int border = slot == hovered ? 0xFFFFD34D : slot == ClientAbilities.selected() ? 0xFF9F7FE0 : 0xFF555555;
            graphics.fill(x, y, x + 22, y + 22, border);
            graphics.fill(x + 1, y + 1, x + 21, y + 21, 0xE0101010);
            int drawX = x + 3;
            ClientAbilities.slot(slot).ifPresent(ability -> {
                AbilityDisplay.drawIcon(graphics, ability, drawX, y + 3);
                if (ClientAbilities.level(ability) == 0) {
                    graphics.fill(drawX, y + 3, drawX + 16, y + 19, 0xB0300000);
                }
            });
        }
        int shown = hovered >= 0 ? hovered : ClientAbilities.selected();
        ClientAbilities.slot(shown).ifPresent(ability -> graphics.drawCenteredString(font, ability.displayName(), width / 2, height / 2 - 4, 0xFFFFFF));
        super.render(graphics, mouseX, mouseY, partialTick);
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
