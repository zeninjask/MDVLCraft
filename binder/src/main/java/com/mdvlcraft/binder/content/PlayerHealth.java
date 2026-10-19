package com.mdvlcraft.binder.content;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Every player has twice the health: a permanent modifier doubling max health after every other bonus. Permanent
 * modifiers are saved with the player but not carried over to the new player entity on respawn, so it is (re)added
 * whenever a player enters a level without it, and the player is then healed to the new maximum.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PlayerHealth {
    private static final UUID DOUBLE = UUID.fromString("5d3c1f0e-7a2b-4c6d-9e8f-1a2b3c4d5e6f");

    private PlayerHealth() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health.getModifier(DOUBLE) == null) {
                health.addPermanentModifier(new AttributeModifier(DOUBLE, "MDVLCraft double health", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
