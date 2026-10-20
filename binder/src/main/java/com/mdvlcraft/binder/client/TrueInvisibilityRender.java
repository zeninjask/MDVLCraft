package com.mdvlcraft.binder.client;

import app.sedu.mc.parties.client.render.PartyRenderer;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;

final class TrueInvisibilityRender {
    private TrueInvisibilityRender() {
    }

    static void onRenderLiving(Pre<?, ?> event) {
        LocalPlayer viewer = Minecraft.getInstance().player;
        LivingEntity entity = event.getEntity();
        if (viewer != null && entity.hasEffect((MobEffect)MobEffectRegistry.TRUE_INVISIBILITY.get()) && hiddenFrom(entity, viewer)) {
            event.setCanceled(true);
        }
    }

    /** Parties clears the invisible flag of the local player and party members before this event, to draw them as ghosts. */
    private static boolean hiddenFrom(LivingEntity entity, LocalPlayer viewer) {
        if (!PartyRenderer.isRenderingGhost) {
            return entity.isInvisibleTo(viewer);
        }

        entity.setInvisible(true);
        boolean hidden = entity.isInvisibleTo(viewer);
        entity.setInvisible(false);
        return hidden;
    }
}
