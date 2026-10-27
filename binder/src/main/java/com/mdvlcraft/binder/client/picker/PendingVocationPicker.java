package com.mdvlcraft.binder.client.picker;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Opens the vocation picker the server asked for once nothing else is on screen (after Origins' layer pickers). */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class PendingVocationPicker {
    private static boolean pending;

    private PendingVocationPicker() {
    }

    public static void request() {
        pending = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && pending && Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
            pending = false;
            VocationPicker.open();
        }
    }
}
