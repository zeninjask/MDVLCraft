package com.mdvlcraft.binder.client;

import app.sedu.mc.parties.client.render.PartyRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Parties clears the invisible flag of the local player and party members at the start of their render to draw them as
 * see-through ghosts, and sets it back at the end. Two things undo that here:
 * - Epic Fight draws its own model from this event (battle mode) and saw the cleared flag, so invisible players were
 *   drawn in full. When Epic Fight is about to draw one, the flag goes back first and Epic Fight hides them itself.
 * - A cancelled render (Epic Fight's, True Invisibility's) returns before Parties sets the flag back, which left it
 *   cleared for good. A cancelled render sets it back.
 */
final class PartiesGhostFlag {
    private PartiesGhostFlag() {
    }

    /** Registered at the highest priority, ahead of Epic Fight's own listener. */
    static void beforeEpicFight(Pre<?, ?> event) {
        if (PartyRenderer.isRenderingGhost && drawnByEpicFight(event.getEntity())) {
            restore(event.getEntity());
        }
    }

    /** Registered at the lowest priority, cancelled events included. */
    static void afterCancel(Pre<?, ?> event) {
        if (event.isCanceled() && PartyRenderer.isRenderingGhost) {
            restore(event.getEntity());
        }
    }

    private static boolean drawnByEpicFight(LivingEntity entity) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        return patch != null && patch.overrideRender() && ClientEngine.getInstance().renderEngine.hasRendererFor(entity);
    }

    private static void restore(LivingEntity entity) {
        PartyRenderer.isRenderingGhost = false;
        entity.setInvisible(true);
    }
}
