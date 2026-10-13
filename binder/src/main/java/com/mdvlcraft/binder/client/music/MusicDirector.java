package com.mdvlcraft.binder.client.music;

import com.mdvlcraft.binder.music.Jingle;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The pack's music: picks a pool from where the player is (menus, villages by day and night, caves, oceans, the wilds),
 * plays a random track from it that is never the one just played, cross-fades when the pool changes, ducks under the
 * skill / class / sleep jingles, and silences every other music source.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class MusicDirector {
    static final float VOLUME = 0.35F;
    private static final float DUCKED = 0.06F;
    private static final float JINGLE_VOLUME = 0.6F;
    private static final int GAP_TICKS = 60;
    private static final String ALCHEMY_SCREEN = "dev.obscuria.elixirum.client.screen.alchemy.AlchemyScreen";
    private static final RandomSource RANDOM = RandomSource.create();
    private static boolean inVillage;
    private static boolean inStructure;
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
            gap = GAP_TICKS;
        }
        if (current == null && !pool.tracks.isEmpty() && gap-- <= 0) {
            current = new MusicTrack(pool, pick(pool.tracks));
            lastTrack = current.track;
            sounds.play(current);
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
            return inStructure ? MusicPool.STRUCTURE : MusicPool.OTHER_DIMENSION;
        }
        if (inVillage) {
            long time = mc.level.getDayTime() % 24000L;
            return time >= 13000L && time < 23000L ? MusicPool.VILLAGE_NIGHT : MusicPool.VILLAGE_DAY;
        }
        BlockPos pos = player.blockPosition();
        if (pos.getY() < mc.level.getSeaLevel() && !mc.level.canSeeSky(pos.above())) {
            return MusicPool.CAVE;
        }
        if (inStructure) {
            return MusicPool.STRUCTURE;
        }
        Holder<Biome> biome = mc.level.getBiome(pos);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) ? MusicPool.OCEAN : MusicPool.WILDS;
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
    }
}
