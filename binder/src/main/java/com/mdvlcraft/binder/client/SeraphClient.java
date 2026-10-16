package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.SeraphFormPacket;
import com.mdvlcraft.binder.seraph.SeraphForm;
import com.mdvlcraft.binder.seraph.SeraphForm.FormPhase;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Seraph Embodiment on the client (seraph/SeraphForm): players in the form are drawn as the Seraphim (SeraphRenderer)
 * instead of their own body (cancelling RenderPlayerEvent also skips Epic Fight's, which renders in RenderLivingEvent);
 * the local player in the form sees no hands, cannot attack or use anything, keeps flying and does not sprint.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class SeraphClient {
    private SeraphClient() {
    }

    public static void handle(SeraphFormPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (packet.phase() == FormPhase.NONE) {
            SeraphForm.CLIENT.remove(packet.entity());
        } else if (packet.cast()) {
            Seen seen = SeraphForm.CLIENT.get(packet.entity());
            if (seen != null) {
                seen.castAt = now;
            }
        } else {
            SeraphForm.CLIENT.put(packet.entity(), new Seen(packet.phase(), now - packet.ticks()));
        }
        Entity entity = level.getEntity(packet.entity());
        if (entity != null) {
            entity.refreshDimensions();
        }
    }

    private static boolean local() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && SeraphForm.CLIENT.containsKey(player.getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Seen seen = SeraphForm.CLIENT.get(player.getId());
        if (seen == null) {
            return;
        }
        event.setCanceled(true);
        if (!player.isInvisibleTo(Minecraft.getInstance().player)) {
            SeraphRenderer.render(player, seen, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (local()) {
            event.setCanceled(true);
        }
    }

    /** No attacking, using or picking: the keys do nothing and the hand does not swing. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (local()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            SeraphForm.CLIENT.clear();
            return;
        }
        SeraphForm.CLIENT.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player));
        for (Player player : level.players()) {
            boolean seraph = SeraphForm.CLIENT.containsKey(player.getId());
            if (seraph != (Math.abs(player.getBbWidth() - SeraphForm.EYE) < 1.0E-4F)) {
                player.refreshDimensions();
            }
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && local()) {
            player.setSprinting(false);
            if (player.getAbilities().mayfly) {
                player.getAbilities().flying = true;
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SeraphForm.CLIENT.clear();
    }
}
