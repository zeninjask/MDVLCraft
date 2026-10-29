package com.mdvlcraft.binder.content;

import com.github.L_Ender.cataclysm.entity.etc.IHomeEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * A Cataclysm boss remembers where it spawned and leaves a boss respawner there when it dies. In the pack's own dimensions
 * (gates, red gates, the demon castle, the boss arena) that spot is the arena's middle, where the reward chest goes, so
 * there it forgets its home as it dies and leaves nothing.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class NoBossRespawner {
    private NoBossRespawner() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof IHomeEntity home
            && event.getEntity().level().dimension().location().getNamespace().equals("mdvlcraft")) {
            home.setHomePos(null);
        }
    }
}
