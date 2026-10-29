package com.mdvlcraft.binder.content;

import Gvoper.reworked.config.EpicFightStaminaInteractionsConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/**
 * Stamina Interactions Reworked sets Max Stamina's base to its start value (100) on a player's first join and after a death,
 * but not after leaving the End through its exit portal, where the new player kept Epic Fight's 15. The base is set back
 * after any respawn and on every login (which also repairs players it already happened to).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class StaminaBase {
    private StaminaBase() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        restore(event.getEntity());
    }

    private static void restore(Player player) {
        AttributeInstance max = player.getAttribute(EpicFightAttributes.MAX_STAMINA.get());
        double base = EpicFightStaminaInteractionsConfig.MAX_START_STAMINA.get();
        if (max != null && max.getBaseValue() != base) {
            max.setBaseValue(base);
        }
    }
}
