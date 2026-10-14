package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.DismissAssimilationPacket;
import com.tiago.antimagic.capability.BlackAstaCapability;
import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/** Pressing right-click while sneaking in Demonic Assimilation asks the server to dismiss it (DemonicAssimilationDismiss). */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class DemonicAssimilationInput {
    private static boolean useHeld;

    private DemonicAssimilationInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean use = mc.player != null && mc.screen == null
            && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (use && !useHeld && mc.player.isShiftKeyDown()
            && mc.player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).map(BlackAstaCapability::isBlackAstaActive).orElse(false)) {
            BinderNetwork.sendToServer(new DismissAssimilationPacket());
        }
        useHeld = use;
    }
}
