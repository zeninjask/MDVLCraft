package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.ExpShareTogglePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The Experience Share toggle under the Social Interactions screen's blocking button, greyed out for party members who aren't the leader (content/ExpShare). */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class ExpShareButton {
    private static final Component BLOCKING_HINT = Component.translatable("gui.socialInteractions.blocking_hint");
    private static boolean on;
    private static boolean decides = true;
    private static Button button;

    private ExpShareButton() {
    }

    public static void set(boolean value, boolean mayChange) {
        on = value;
        decides = mayChange;
        if (button != null) {
            button.setMessage(label());
            button.active = decides;
        }
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SocialInteractionsScreen)) {
            return;
        }
        Button hint = event.getListenersList().stream()
            .filter(widget -> widget instanceof Button found && found.getMessage().equals(BLOCKING_HINT))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Social Interactions screen has no blocking button"));
        button = Button.builder(label(), pressed -> BinderNetwork.sendToServer(new ExpShareTogglePacket(true)))
            .bounds(hint.getX(), hint.getY() + hint.getHeight() + 4, hint.getWidth(), hint.getHeight())
            .tooltip(Tooltip.create(Component.translatable("gui.mdvlcraft.exp_share.tooltip")))
            .build();
        button.active = decides;
        event.addListener(button);
        BinderNetwork.sendToServer(new ExpShareTogglePacket(false));
    }

    private static Component label() {
        return Component.translatable("gui.mdvlcraft.exp_share", on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }
}
