package com.mdvlcraft.binder.client;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Selected wheel ability, mana bar and cast progress in the bottom-right corner (the pack's HUD mod rearranges the
 * hotbar area). Hidden until the player has an ability.
 */
final class AbilityHud implements IGuiOverlay {
    static final AbilityHud INSTANCE = new AbilityHud();

    private AbilityHud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || ClientAbilities.granted().isEmpty()) {
            return;
        }
        int x = screenWidth - 26;
        int y = screenHeight - 26;
        MedievalGui.frame(graphics, "challenge", "unlocked", x - 2, y - 2, 26);
        ClientAbilities.slot(ClientAbilities.selected()).ifPresent(ability -> {
            AbilityDisplay.drawIcon(graphics, ability, x + 3, y + 3);
            if (ClientAbilities.level(ability) == 0) {
                graphics.fill(x + 3, y + 3, x + 19, y + 19, 0xB0300000);
            }
        });
        graphics.drawString(minecraft.font, String.valueOf(ClientAbilities.selected() + 1), x - 1, y - 1, 0xFFF0D9A0);

        double maxMana = minecraft.player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        float filled = maxMana <= 0 ? 0 : (float) Math.min(1, ClientMagicData.getPlayerMana() / maxMana);
        int barTop = y - 6;
        graphics.fill(x - 1, barTop - 1, x + 23, barTop + 4, 0xFF4A321E);
        graphics.fill(x, barTop, x + 22, barTop + 3, 0xFF101030);
        graphics.fill(x, barTop, x + Math.round(22 * filled), barTop + 3, 0xFF3F8CFF);
        if (ClientMagicData.isCasting()) {
            graphics.fill(x, barTop - 4, x + Math.round(22 * ClientMagicData.getCastCompletionPercent()), barTop - 2, 0xFFD6AA42);
        }
    }
}
