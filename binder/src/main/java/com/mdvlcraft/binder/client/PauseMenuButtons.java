package com.mdvlcraft.binder.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The pause menu has no "Ares HUD Config" button; Ares HUD adds it with no setting to turn it off. */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class PauseMenuButtons {
    private static final String ARES_HUD_CONFIG = "Ares HUD Config";

    private PauseMenuButtons() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof PauseScreen) {
            event.getListenersList().stream()
                .filter(widget -> widget instanceof AbstractWidget button && button.getMessage().getString().equals(ARES_HUD_CONFIG))
                .toList()
                .forEach(event::removeListener);
        }
    }
}
