package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gate.GateDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;

/**
 * No maps inside a gate (any of the gate dimensions): the dungeon is to be found, not read off a map.
 * - Xaero's Minimap is hidden through its own "no minimap" switch (the one its "nominimap" server message sets),
 *   which skips the minimap and its radar; it is switched back on when the player leaves the gate.
 * - Xaero's World Map doesn't open (its screen is cancelled, with a note on the action bar).
 * Xaero's classes are touched only when the minimap is installed.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class GateMapBlock {
    private static final boolean MINIMAP = ModList.get().isLoaded("xaerominimapfair") || ModList.get().isLoaded("xaerominimap");
    private static final String WORLD_MAP_SCREEN = "xaero.map.gui.GuiMap";

    private GateMapBlock() {
    }

    private static boolean inGate() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && GateDimensions.isGate(minecraft.level.dimension());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (MINIMAP && event.phase == Phase.END) {
            Minimap.update(inGate());
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass().getName().equals(WORLD_MAP_SCREEN) && inGate()) {
            event.setCanceled(true);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gate.mdvlcraft.no_map"), true);
            }
        }
    }

    /** Kept apart so Xaero's classes load only when the minimap is installed. */
    private static final class Minimap {
        /** The processor this hid the minimap in; a new session (reconnect) starts with the minimap shown. */
        private static MinimapProcessor hidden;

        static void update(boolean gate) {
            XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            MinimapProcessor processor = session == null ? null : session.getMinimapProcessor();
            if (processor == null) {
                hidden = null;
            } else if (gate) {
                if (!processor.getNoMinimapMessageReceived()) {
                    processor.setNoMinimapMessageReceived(true);
                    hidden = processor;
                }
            } else if (hidden == processor) {
                processor.setNoMinimapMessageReceived(false);
                hidden = null;
            }
        }
    }
}
