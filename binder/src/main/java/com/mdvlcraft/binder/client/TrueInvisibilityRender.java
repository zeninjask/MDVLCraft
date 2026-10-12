package com.mdvlcraft.binder.client;

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
        if (viewer != null && entity.hasEffect((MobEffect)MobEffectRegistry.TRUE_INVISIBILITY.get()) && entity.isInvisibleTo(viewer)) {
            event.setCanceled(true);
        }
    }
}
