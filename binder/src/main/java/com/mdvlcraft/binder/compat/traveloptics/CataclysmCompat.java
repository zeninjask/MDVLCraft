package com.mdvlcraft.binder.compat.traveloptics;

import com.github.L_Ender.cataclysm.client.particle.Options.CircleLightningParticleOptions;
import com.github.L_Ender.cataclysm.client.particle.Options.RingParticleOptions;
import com.github.L_Ender.cataclysm.client.particle.RingParticle.EnumRingBehavior;

public final class CataclysmCompat {
    private CataclysmCompat() {
    }

    public static CircleLightningParticleOptions circle(int r, int g, int b) {
        return new CircleLightningParticleOptions(1.0F, r, g, b);
    }

    public static RingParticleOptions ring(
        float yaw, float pitch, int duration, float r, float g, float b, float a, float scale, boolean facesCamera, EnumRingBehavior behavior
    ) {
        return new RingParticleOptions(
            yaw, pitch, duration, Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F), a, scale, facesCamera, behavior.ordinal()
        );
    }

    public static double harbingerHealingMultiplier() {
        return 1.0;
    }

    public static boolean harbingerLightFire() {
        return true;
    }
}
