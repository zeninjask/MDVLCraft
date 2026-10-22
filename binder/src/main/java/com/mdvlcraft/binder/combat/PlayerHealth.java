package com.mdvlcraft.binder.combat;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Players have 20% less max health (after every bonus: classes, origins, gear). Re-added on login and respawn. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PlayerHealth {
    private static final UUID MODIFIER = UUID.fromString("3e1f6a52-8c0d-4b7e-9a35-d2c47f1b6e90");

    private PlayerHealth() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    private static void apply(Player player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(MODIFIER) == null) {
            health.addTransientModifier(new AttributeModifier(MODIFIER, "MDVLCraft player health", -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
