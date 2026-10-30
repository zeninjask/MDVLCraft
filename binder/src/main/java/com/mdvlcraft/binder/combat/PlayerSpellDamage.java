package com.mdvlcraft.binder.combat;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Every spell a player casts (Iron's and every spell built on it) deals 10% less damage. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PlayerSpellDamage {
    private static final float MULTIPLIER = 0.9F;

    private PlayerSpellDamage() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().getEntity() instanceof Player) {
            event.setAmount(event.getAmount() * MULTIPLIER);
        }
    }
}
