package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.client.doppelganger.DoppelgangerClientSetup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientAbilitySetup {
    private ClientAbilitySetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AbilityKeys::onRegisterKeys);
        modBus.addListener(ClientAbilitySetup::onRegisterOverlays);
        modBus.addListener(ClientAbilitySetup::onClientSetup);
        DoppelgangerClientSetup.register(modBus);
        com.mdvlcraft.binder.client.antimagic.DemonicAssimilationLayers.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(AbilityKeys::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RemovedKeys::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(TracingDashAnimation::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(MenuNav::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(MenuNav::onScreenRender);
        MinecraftForge.EVENT_BUS.addListener(MenuNav::onMouseClicked);
        MinecraftForge.EVENT_BUS.addListener(ClientAbilitySetup::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(DqGui::onTooltipColour);
        MinecraftForge.EVENT_BUS.addListener(SpellPreviews::onClientTick);
        modBus.addListener(DqRecruits::onAddPackFinders);
        modBus.addListener(BdHillFix::onAddPackFinders);
        MinecraftForge.EVENT_BUS.addListener(DqRecruits::onRenderPre);
        MinecraftForge.EVENT_BUS.addListener(DqRecruits::onRenderPost);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, Pre.class, TrueInvisibilityRender::onRenderLiving);
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ability_hud", AbilityHud.INSTANCE);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AbilityKeys.logConflicts(Minecraft.getInstance()));
    }

    private static void onLoggingOut(LoggingOut event) {
        ClientAbilities.reset();
        ClientStances.reset();
        ClientTechniqueCooldowns.reset();
    }
}
