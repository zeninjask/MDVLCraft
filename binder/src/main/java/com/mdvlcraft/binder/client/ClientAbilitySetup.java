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
        modBus.addListener(DqSkins::onAddPackFinders);
        modBus.addListener(BdHillFix::onAddPackFinders);
        MinecraftForge.EVENT_BUS.addListener(DqSkins::onRenderPre);
        MinecraftForge.EVENT_BUS.addListener(DqSkins::onRenderPost);
        MinecraftForge.EVENT_BUS.addListener(PartyPanels::onRenderOverlay);
        MinecraftForge.EVENT_BUS.addListener(DqInventoryScreen::onScreenOpening);
        MinecraftForge.EVENT_BUS.addListener(DqInventoryScreen::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(ChatPosition::onChat);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, Pre.class, PartiesGhostFlag::beforeEpicFight);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, Pre.class, TrueInvisibilityRender::onRenderLiving);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, Pre.class, PartiesGhostFlag::afterCancel);
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ability_hud", AbilityHud.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "party_panels", PartyPanels.INSTANCE);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AbilityKeys.logConflicts(Minecraft.getInstance()));
    }

    private static void onLoggingOut(LoggingOut event) {
        ClientAbilities.reset();
        ClientStances.reset();
        ClientTechniqueCooldowns.reset();
        ClientArchetypes.reset();
    }
}
