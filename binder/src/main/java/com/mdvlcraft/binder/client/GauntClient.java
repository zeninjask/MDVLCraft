package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gaunt.GauntForm;
import com.mdvlcraft.binder.network.GauntFormPacket;
import com.mdvlcraft.binder.seraph.SeraphForm.FormPhase;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
 * Gaunt Vessel on the client (gaunt/GauntForm): players in the form are drawn by GauntRenderer instead of their own body
 * (cancelling RenderPlayerEvent also skips Epic Fight's); souls leak from them at every snapping bone and burst out when
 * the body tears into the Haunted Lord. Every client plays the Haunted Lord's sounds (assets/mdvlcraft/sounds.json,
 * tools/haunted_lord/sounds.py) at the points of its animations they belong to: a bone snap at each of the player's
 * snaps and the Haunted Lord's own as it unfolds, a breath per idle cycle, whispers per cast cycle, a foot drag per walking
 * step, a joint crack per hand planted in the crawl, and a screech every few seconds of sprinting. The local player in
 * the form sees no hands and cannot attack or use anything.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class GauntClient {
    /** Each changing player's point in the entering sequence last tick, to find the snaps passed since. */
    private static final Map<Integer, Float> LAST = new HashMap<>();
    /** Each player's animation last tick in the form, to find the sound cues passed since. */
    private static final Map<Integer, GauntRenderer.Pick> LAST_PICK = new HashMap<>();
    private static final Map<Integer, Long> NEXT_SCREECH = new HashMap<>();
    /** The Haunted Lord's own cracks as it unfolds (ticks of entering: SNAP_TICKS + its transform's snaps, art/haunted_lord/source/anims.py). */
    private static final int[] EMERGE_SNAPS = {70, 74, 78, 81, 84, 86, 89};
    /** Where in each animation (seconds) its sound plays: idle and cast at the start of the breath, walk at each foot
     * contact, crawl as each hand is planted. */
    private static final Map<String, float[]> CUES = Map.of("idle", new float[] {0.0F}, "cast", new float[] {0.0F},
        "walk", new float[] {0.0F, 1.2F}, "crawl", new float[] {0.7F, 2.1F});
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    private GauntClient() {
    }

    public static void handle(GauntFormPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (packet.phase() == FormPhase.NONE) {
            GauntForm.CLIENT.remove(packet.entity());
        } else if (packet.cast()) {
            Seen seen = GauntForm.CLIENT.get(packet.entity());
            if (seen != null) {
                seen.castAt = now;
            }
        } else {
            GauntForm.CLIENT.put(packet.entity(), new Seen(packet.phase(), now - packet.ticks()));
        }
    }

    private static boolean local() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && GauntForm.CLIENT.containsKey(player.getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Seen seen = GauntForm.CLIENT.get(player.getId());
        if (seen == null) {
            return;
        }
        event.setCanceled(true);
        if (!player.isInvisibleTo(Minecraft.getInstance().player)) {
            GauntRenderer.render(player, seen, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                (PlayerRenderer)event.getRenderer());
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
            GauntForm.CLIENT.clear();
            LAST.clear();
            LAST_PICK.clear();
            NEXT_SCREECH.clear();
            return;
        }
        GauntForm.CLIENT.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player));
        LAST.keySet().retainAll(GauntForm.CLIENT.keySet());
        NEXT_SCREECH.keySet().retainAll(GauntForm.CLIENT.keySet());
        long now = level.getGameTime();
        LAST_PICK.keySet().retainAll(GauntForm.CLIENT.keySet());
        for (Map.Entry<Integer, Seen> entry : GauntForm.CLIENT.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof Player player)) {
                continue;
            }
            Seen seen = entry.getValue();
            if (seen.phase == FormPhase.ACTIVE) {
                LAST.remove(entry.getKey());
                cues(level, player, seen, now);
                continue;
            }
            LAST_PICK.remove(entry.getKey());
            float time = GauntForm.enteringTime(seen.phase, now - seen.since);
            Float last = LAST.put(entry.getKey(), time);
            if (last == null) {
                continue;
            }
            float lo = Math.min(last, time);
            float hi = Math.max(last, time);
            double y = player.getY() + GauntRenderer.lift(time) + 1.1;
            if (time < GauntForm.SNAP_TICKS) {
                burst(level, player, y, ParticleTypes.SOUL, 1, 0.3, 0.02);
            }
            for (int snap : GauntForm.SNAPS) {
                if (snap > lo && snap <= hi) {
                    burst(level, player, y, ParticleTypes.SOUL, 4, 0.4, 0.06);
                    burst(level, player, y, ParticleTypes.SCULK_SOUL, 2, 0.3, 0.05);
                    play(level, player, "snap", 1.3F, 0.85F + 0.3F * level.getRandom().nextFloat());
                }
            }
            for (int snap : EMERGE_SNAPS) {
                if (snap > lo && snap <= hi) {
                    play(level, player, "snap", 1.1F, 0.7F + 0.2F * level.getRandom().nextFloat());
                }
            }
            if (GauntForm.SNAP_TICKS > lo && GauntForm.SNAP_TICKS <= hi) {
                burst(level, player, y, ParticleTypes.SOUL_FIRE_FLAME, 45, 0.6, 0.18);
                burst(level, player, y, ParticleTypes.SOUL, 25, 0.6, 0.12);
                burst(level, player, y, ParticleTypes.SCULK_SOUL, 12, 0.5, 0.1);
            }
        }
    }

    /** The form's sounds that belong to points of its animations, and the sprint's screeches. */
    private static void cues(ClientLevel level, Player player, Seen seen, long now) {
        GauntRenderer.Pick pick = GauntRenderer.pick(player, seen, 0.0F, now);
        GauntRenderer.Pick last = LAST_PICK.put(player.getId(), pick);
        if (pick.animation().equals("sprint")) {
            Long next = NEXT_SCREECH.get(player.getId());
            if (next == null || now >= next) {
                play(level, player, "sprint", 1.0F, 0.9F + 0.2F * level.getRandom().nextFloat());
                NEXT_SCREECH.put(player.getId(), now + 40 + level.getRandom().nextInt(30));
            }
        } else {
            NEXT_SCREECH.remove(player.getId());
        }
        if (last == null || !last.animation().equals(pick.animation())) {
            return;
        }
        for (float cue : CUES.getOrDefault(pick.animation(), new float[0])) {
            boolean passed = last.time() <= pick.time() ? cue > last.time() && cue <= pick.time() : cue > last.time() || cue <= pick.time();
            if (passed && last.time() != pick.time()) {
                float pitch = pick.animation().equals("walk") || pick.animation().equals("crawl") ? 0.85F + 0.3F * level.getRandom().nextFloat() : 1.0F;
                play(level, player, pick.animation(), pick.animation().equals("walk") ? 0.7F : 0.9F, pitch);
            }
        }
    }

    private static void play(ClientLevel level, Player player, String sound, float volume, float pitch) {
        SoundEvent event = SOUNDS.computeIfAbsent(sound,
            name -> SoundEvent.createVariableRangeEvent(new ResourceLocation("mdvlcraft", "haunted_lord." + name)));
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static void burst(ClientLevel level, Player player, double y, ParticleOptions particle, int count, double spread, double speed) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double dx = random.nextGaussian();
            double dy = random.nextGaussian();
            double dz = random.nextGaussian();
            level.addParticle(particle, player.getX() + dx * spread, y + dy * spread, player.getZ() + dz * spread, dx * speed, dy * speed + 0.02,
                dz * speed);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GauntForm.CLIENT.clear();
        LAST.clear();
        LAST_PICK.clear();
        NEXT_SCREECH.clear();
    }
}
