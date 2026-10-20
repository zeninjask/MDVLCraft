package com.mdvlcraft.binder.util;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** TEMPORARY: logs who makes a mob target an invisible player, to find the path that ignores invisibility. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class InvisibleTargetDebug {
    private InvisibleTargetDebug() {
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && player.isInvisible()) {
            LogUtils.getLogger().warn("[invis-debug] {} targets invisible {} ({})", event.getEntity(), player.getName().getString(), event.getTargetType(),
                new Throwable("target stack"));
        }
    }
}
