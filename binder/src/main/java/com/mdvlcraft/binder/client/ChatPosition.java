package com.mdvlcraft.binder.client;

import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

/**
 * The chat sits just above the Ares HUD hotbar (bottom left) instead of over its top row. Its bottom edge is 10 px
 * below Forge's chat position (height - 40 by default). The hotbar's top is a fraction of the screen height
 * (hotbarDefaultProperties y in the pack's ares_hud-client.toml, Layout_DEFAULT). Clicks and hovers on chat lines are
 * moved by the same amount (mixin/ChatComponentMixin).
 */
public final class ChatPosition {
    private static final double HOTBAR_TOP = 0.8694779277;
    private static final int DEFAULT_POS_ABOVE_BOTTOM = 40;
    private static final int CHAT_BOTTOM_BELOW_POS = 10;
    private static final int GAP = 3;

    private ChatPosition() {
    }

    /** How far the chat is raised on a screen this many GUI pixels high. */
    public static int raise(int height) {
        int hotbarTop = (int)(HOTBAR_TOP * height);
        return Math.max(0, height - DEFAULT_POS_ABOVE_BOTTOM + CHAT_BOTTOM_BELOW_POS - (hotbarTop - GAP));
    }

    static void onChat(CustomizeGuiOverlayEvent.Chat event) {
        event.setPosY(event.getPosY() - raise(event.getWindow().getGuiScaledHeight()));
    }
}
