package com.mdvlcraft.binder.client.music;

import com.mdvlcraft.binder.music.Jingle;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * The pack's music: picks a pool from where the player is (menus, villages by day and night, caves, oceans, the wilds),
 * plays a random track from it that is never the one just played, waits 30 seconds to 3 minutes between tracks,
 * cross-fades straight to the new pool when the pool changes, ducks under the
 * skill / class / sleep jingles, and silences every other music source.
 *
 * Changes of place have a 15-block buffer so walking along an edge doesn't switch back and forth: the server only
 * reports leaving a village or structure 15 blocks past its last piece (music/MusicPlaces), and the cave, ocean and
 * wilds music only changes once the player is 15 blocks from where the old one last applied.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class MusicDirector {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final float VOLUME = 0.7F;
    private static final float DUCKED = 0.12F;
    private static final float JINGLE_VOLUME = 1.0F;
    /** Silence after a track ends, before the next one: 30 seconds to 3 minutes. */
    private static final int MIN_GAP_TICKS = 30 * 20;
    private static final int MAX_GAP_TICKS = 180 * 20;
    private static final String ALCHEMY_SCREEN = "dev.obscuria.elixirum.client.screen.alchemy.AlchemyScreen";
    private static final RandomSource RANDOM = RandomSource.create();
    private static boolean inVillage;
    private static boolean inStructure;
    private static final double TERRAIN_BUFFER = 15.0;
    /** The cave, ocean or wilds pool playing, and where the player last stood in its area. */
    private static MusicPool terrain;
    private static Vec3 terrainAt;
    private static MusicTrack current;
    private static String lastTrack;
    private static int gap;
    private static SoundInstance jingle;
    private static boolean wasSleeping;

    private MusicDirector() {
    }

    static ResourceLocation sound(String track) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", "music." + track);
    }

    static float targetVolume() {
        return jingle == null ? VOLUME : DUCKED;
    }

    public static void setPlace(boolean village, boolean structure) {
        inVillage = village;
        inStructure = structure;
    }

    public static void playJingle(Jingle cue) {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        if (jingle != null) {
            sounds.stop(jingle);
        }
        jingle = new SimpleSoundInstance(sound(cue.track), SoundSource.MUSIC, JINGLE_VOLUME, 1.0F, RANDOM, false, 0,
            SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        sounds.play(jingle);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SoundManager sounds = mc.getSoundManager();
        boolean sleeping = mc.player != null && mc.player.isSleeping();
        if (sleeping && !wasSleeping) {
            playJingle(Jingle.SLEEP);
        }
        wasSleeping = sleeping;
        if (jingle != null && !sounds.isActive(jingle)) {
            jingle = null;
        }
        MusicPool pool = pool(mc);
        if (current != null && current.pool != pool) {
            current.fadeOut();
            current = null;
            gap = 0;
        }
        if (current != null && !sounds.isActive(current)) {
            current = null;
            gap = MIN_GAP_TICKS + RANDOM.nextInt(MAX_GAP_TICKS - MIN_GAP_TICKS + 1);
        }
        if (current == null && !pool.tracks.isEmpty() && gap-- <= 0) {
            current = new MusicTrack(pool, pick(pool.tracks));
            lastTrack = current.track;
            sounds.play(current);
            LOGGER.info("Music: {} playing {} (active {})", pool, current.track, sounds.isActive(current));
        }
    }

    private static String pick(List<String> tracks) {
        List<String> choices = tracks.size() > 1 ? tracks.stream().filter(t -> !t.equals(lastTrack)).toList() : tracks;
        return choices.get(RANDOM.nextInt(choices.size()));
    }

    private static MusicPool pool(Minecraft mc) {
        Screen screen = mc.screen;
        if (screen instanceof ConnectScreen || screen instanceof LevelLoadingScreen || screen instanceof ReceivingLevelScreen) {
            return MusicPool.LOADING;
        }
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return MusicPool.MENU;
        }
        if (screen != null && screen.getClass().getName().equals(ALCHEMY_SCREEN)) {
            return MusicPool.ALCHEMY;
        }
        if (mc.level.dimension() != Level.OVERWORLD) {
            terrain = null;
            return inStructure ? MusicPool.STRUCTURE : MusicPool.OTHER_DIMENSION;
        }
        if (inVillage) {
            terrain = null;
            long time = mc.level.getDayTime() % 24000L;
            return time >= 13000L && time < 23000L ? MusicPool.VILLAGE_NIGHT : MusicPool.VILLAGE_DAY;
        }
        BlockPos pos = player.blockPosition();
        boolean cave = pos.getY() < mc.level.getSeaLevel() && !mc.level.canSeeSky(pos.above());
        if (inStructure && !cave) {
            terrain = null;
            return MusicPool.STRUCTURE;
        }
        Holder<Biome> biome = mc.level.getBiome(pos);
        MusicPool here = cave ? MusicPool.CAVE : biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) ? MusicPool.OCEAN : MusicPool.WILDS;
        return buffered(here, player.position());
    }

    /** The cave, ocean or wilds pool, kept until the player is TERRAIN_BUFFER blocks from where it last applied. */
    private static MusicPool buffered(MusicPool here, Vec3 position) {
        if (terrain == null || here == terrain || position.distanceTo(terrainAt) >= TERRAIN_BUFFER) {
            terrain = here;
            terrainAt = position;
        }
        return terrain;
    }

    /** Vanilla and other mods' music (music category, or any sound named *music* other than jukebox discs) never plays. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound.getLocation().getNamespace().equals("mdvlcraft")) {
            return;
        }
        String path = sound.getLocation().getPath();
        if (sound.getSource() == SoundSource.MUSIC || path.contains("music") && !path.contains("music_disc")) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        setPlace(false, false);
        terrain = null;
    }
}
