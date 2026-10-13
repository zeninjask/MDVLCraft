package com.mdvlcraft.binder.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** One background track that fades in to the director's volume, follows its ducking, and fades out before stopping. */
final class MusicTrack extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 60;
    final MusicPool pool;
    final String track;
    private float level;
    private boolean fadingOut;

    MusicTrack(MusicPool pool, String track) {
        super(SoundEvent.createVariableRangeEvent(MusicDirector.sound(track)), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.pool = pool;
        this.track = track;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    void fadeOut() {
        this.fadingOut = true;
    }

    @Override
    public void tick() {
        float target = this.fadingOut ? 0.0F : MusicDirector.targetVolume();
        float step = MusicDirector.VOLUME / FADE_TICKS;
        this.level = this.level < target ? Math.min(target, this.level + step) : Math.max(target, this.level - step);
        if (this.fadingOut && this.level <= 0.0F) {
            this.stop();
        }
    }

    @Override
    public float getVolume() {
        return this.level;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
