package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.content.KeepSlotOpen;
import com.mdvlcraft.binder.content.MageHunterSlot;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The player's settings, opened from the Status page (CharacterScreen). For now only Keep Slot Open (content/KeepSlotOpen). */
public class SettingsScreen extends Screen {
    private static final int W = 260;
    private static final int H = 130;
    private final Screen parent;
    private DqGui.Button keepSlotOpen;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        this.keepSlotOpen = this.addRenderableWidget(new DqGui.Button(x + 14, y + 30, W - 28, 16, this.keepSlotOpenLabel(), () -> {
            boolean on = !KeepSlotOpen.isOn(Minecraft.getInstance().player);
            KeepSlotOpen.setClient(on);
            BinderNetwork.sendToServer(new SettingsPacket(on));
            this.keepSlotOpen.setMessage(this.keepSlotOpenLabel());
        }));
        this.addRenderableWidget(new DqGui.Button(x + W - 66, y + H - 22, 56, 14, Component.translatable("screen.mdvlcraft.menu.back"),
            () -> Minecraft.getInstance().setScreen(this.parent)));
    }

    private Component keepSlotOpenLabel() {
        return Component.translatable("screen.mdvlcraft.settings.keep_slot_open",
            Component.translatable(KeepSlotOpen.isOn(Minecraft.getInstance().player) ? "options.on" : "options.off"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AbilityKeys.MENU.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        DqGui.panel(g, this.font, this.title, x, y, W, H);
        String key = MageHunterSlot.isMageHunter(Minecraft.getInstance().player)
            ? "screen.mdvlcraft.settings.keep_slot_open.samurai" : "screen.mdvlcraft.settings.keep_slot_open.desc";
        int line = y + 52;
        for (FormattedCharSequence text : this.font.split(Component.translatable(key), W - 32)) {
            g.drawString(this.font, text, x + 16, line, DqGui.MUTED, false);
            line += 11;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
