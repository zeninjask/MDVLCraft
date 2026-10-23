package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.PickShadowPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Formless Sword's sword wheel: the weapons bound in the Shadow Arsenal; release the cast key (or click) on one to pick it. */
public final class SwordWheelScreen extends Screen {
    private static final int RADIUS = 56;

    public SwordWheelScreen() {
        super(Component.translatable("screen.mdvlcraft.sword_wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static double angle(int slot) {
        return -Math.PI / 2 + slot * 2 * Math.PI / 8.0;
    }

    private int hovered(double mouseX, double mouseY) {
        double dx = mouseX - this.width / 2.0;
        double dy = mouseY - this.height / 2.0;
        if (dx * dx + dy * dy < 196.0) {
            return -1;
        }
        double turn = (Math.atan2(dy, dx) + Math.PI / 2) / (Math.PI * 2);
        return Math.floorMod((int)Math.round(turn * 8.0), 8);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hovered = this.hovered(mouseX, mouseY);
        DqGui.disc(graphics, this.width / 2, this.height / 2, 76, false);
        for (int slot = 0; slot < 8; slot++) {
            int x = this.width / 2 + (int)Math.round(RADIUS * Math.cos(angle(slot))) - 12;
            int y = this.height / 2 + (int)Math.round(RADIUS * Math.sin(angle(slot))) - 12;
            boolean bound = ClientArsenal.wheel(slot).isPresent();
            boolean picked = ClientArsenal.wheel(slot).map(item -> ClientArsenal.id(item).equals(ClientArsenal.pick())).orElse(false);
            DqGui.slot(graphics, x, y, 24, slot == hovered || hovered < 0 && picked ? DqGui.Slot.PICKED : bound ? DqGui.Slot.FILLED : DqGui.Slot.EMPTY);
            ClientArsenal.wheel(slot).ifPresent(item -> graphics.renderItem(new ItemStack(item), x + 4, y + 4));
        }
        DqGui.disc(graphics, this.width / 2, this.height / 2, 18, true);
        ClientArsenal.wheel(hovered).ifPresent(item -> {
            graphics.renderItem(new ItemStack(item), this.width / 2 - 8, this.height / 2 - 8);
            Component name = item.getDescription();
            int textWidth = this.font.width(name);
            int bannerTop = this.height / 2 + RADIUS + 26;
            DqGui.window(graphics, this.width / 2 - textWidth / 2 - 6, bannerTop, textWidth + 12, 18);
            graphics.drawString(this.font, name, this.width / 2 - textWidth / 2, bannerTop + 5, DqGui.TEXT, false);
        });
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void choose(double mouseX, double mouseY) {
        ClientArsenal.wheel(this.hovered(mouseX, mouseY))
            .ifPresent(item -> BinderNetwork.sendToServer(new PickShadowPacket(ClientArsenal.id(item))));
        this.onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeys.CAST.matches(keyCode, scanCode)) {
            double scale = (double)this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
            this.choose(this.minecraft.mouseHandler.xpos() * scale, this.minecraft.mouseHandler.ypos() * scale);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (AbilityKeys.CAST.matchesMouse(button)) {
            this.choose(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.choose(mouseX, mouseY);
        return true;
    }
}
