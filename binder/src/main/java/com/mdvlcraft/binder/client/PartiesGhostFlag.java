package com.mdvlcraft.binder.client;

import app.sedu.mc.parties.client.render.PartyRenderer;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;

/**
 * Parties clears an invisible entity's flag at the start of its render to draw it as a ghost, and sets it back at the end.
 * A cancelled render (Epic Fight draws its own model, True Invisibility hides it) returns before that end, which left the
 * flag cleared for good: invisible players and party members were drawn in full. A cancelled render sets it back here.
 */
final class PartiesGhostFlag {
    private PartiesGhostFlag() {
    }

    static void onRenderLiving(Pre<?, ?> event) {
        if (event.isCanceled() && PartyRenderer.isRenderingGhost) {
            PartyRenderer.isRenderingGhost = false;
            event.getEntity().setInvisible(true);
        }
    }
}
