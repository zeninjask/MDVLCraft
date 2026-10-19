package com.mdvlcraft.binder.content;

import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Beds work in the day as well: sleeping through the day skips to nightfall, sleeping through the night skips to morning
 * as usual. Everything else about sleeping (monsters nearby, the share of players needed) stays vanilla.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DaySleep {
    private static final long DAY = 24000L;
    /** When vanilla stops letting players sleep in the evening (Level.isDay), and the time a day sleep wakes at. */
    private static final long DUSK = 12542L;
    private static final long NIGHTFALL = 13000L;

    private DaySleep() {
    }

    @SubscribeEvent
    public static void onSleepCheck(SleepingTimeCheckEvent event) {
        event.setResult(Result.ALLOW);
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        long now = event.getLevel().getLevelData().getDayTime();
        if (now % DAY < DUSK) {
            event.setTimeAddition(now - now % DAY + NIGHTFALL);
        }
    }
}
