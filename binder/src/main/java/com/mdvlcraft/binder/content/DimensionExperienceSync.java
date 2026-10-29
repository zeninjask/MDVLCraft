package com.mdvlcraft.binder.content;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Teleporting to another dimension (gates, the boss arena, return crystals) gives the client a fresh player with no
 * experience, and vanilla only resends it after a portal, so the level read 0 until the next experience point. It is
 * resent on every change of dimension.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DimensionExperienceSync {
    private DimensionExperienceSync() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
        }
    }
}
