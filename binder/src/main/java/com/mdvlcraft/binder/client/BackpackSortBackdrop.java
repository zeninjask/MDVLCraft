package com.mdvlcraft.binder.client;

import com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Inventory Profiles Next's sort buttons sit centred above the backpack's name tab (config/inventoryprofilesnext/
 * integrationHints/backpacked.json: horizontalOffset 62 and top -19 put its four 10-wide buttons, 12 apart, 4 to 14 above
 * the screen's top, which is the tab's top). This draws a backdrop behind them in the tab's colours, joined to the tab,
 * before anything else on the screen: Inventory Profiles Next draws its buttons before the container background
 * events, so anything drawn there would cover them, and the backpack screen has no dimmed background to follow.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class BackpackSortBackdrop {
    private static final int OUTLINE = 0xFF0B0700;
    private static final int EDGE = 0xFFDBBE9E;
    private static final int HIGHLIGHT = 0xFFF2DEC7;
    private static final int FILL = 0xFFEFDBC4;
    private static final int HALF_WIDTH = 27;
    private static final int HEIGHT = 18;

    private BackpackSortBackdrop() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof BackpackScreen screen)) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int centre = screen.getGuiLeft() + screen.getXSize() / 2;
        int x0 = centre - HALF_WIDTH;
        int x1 = centre + HALF_WIDTH;
        int y0 = screen.getGuiTop() - HEIGHT;
        int y1 = screen.getGuiTop() + 1;   // over the tab's top outline, so the two read as one piece
        g.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
        g.fill(x0, y0 + 1, x1, y1, OUTLINE);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1, EDGE);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1, FILL);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, HIGHLIGHT);
    }
}
