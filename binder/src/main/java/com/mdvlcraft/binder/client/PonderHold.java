package com.mdvlcraft.binder.client;

import com.p1nero.iss_ponder.client.ClientPreviewController;
import io.redspace.ironsspellbooks.api.item.IScroll;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/**
 * Whether the player is holding Ponder's key (W) over something that can be pondered: Ponder reports every frame its
 * tooltip is drawn for a stack with scenes (the Epic Fight weapons), ISS Ponder previews any spell scroll from its
 * own tooltip, and the skill trees and abilities screen preview their spells (SpellPreviews). While a spell preview
 * opens or is open, nothing walks. InvMove then leaves movement off (mixin/invmove/InvMoveMixin), so holding W to ponder in the inventory
 * doesn't walk the player.
 */
public final class PonderHold {
    /** A little over a frame at low frame rates. */
    private static final long HOVER_MS = 150L;
    private static long hoveredAt = Long.MIN_VALUE;

    private PonderHold() {
    }

    static void register() {
        PonderTooltipHandler.registerHoveredPonderStackCallback(stack -> hoveredAt = Util.getMillis());
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            if (event.getItemStack().getItem() instanceof IScroll) {
                hoveredAt = Util.getMillis();
            }
        });
    }

    public static boolean active() {
        if (ClientPreviewController.isPending() || ClientPreviewController.isPreviewOpen()) {
            return true;
        }
        boolean hovering = Util.getMillis() - hoveredAt <= HOVER_MS || SpellPreviews.hovering();
        return hovering && PonderKeybinds.PONDER.isDown();
    }
}
