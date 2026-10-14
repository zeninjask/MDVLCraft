package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.content.DemonSwordArts;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.DemonSwordArtPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/** Pressing both mouse buttons with a demon sword in hand casts its art (DemonSwordArts), once per press. */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class DemonSwordInput {
    private static boolean bothHeld;

    private DemonSwordInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        boolean both = mc.player != null && mc.screen == null
            && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (both && !bothHeld && DemonSwordArts.isDemonSword(mc.player.getMainHandItem())) {
            BinderNetwork.sendToServer(new DemonSwordArtPacket());
        }
        bothHeld = both;
    }
}
