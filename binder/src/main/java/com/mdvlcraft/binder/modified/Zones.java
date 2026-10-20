package com.mdvlcraft.binder.modified;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Areas a Modified ability leaves on the ground for a while (a banner, a campfire): every `interval` ticks the zone pulses,
 * and when it ends its marker entity is removed and `end` runs. Zones live only while the server runs; a marker left over
 * from a stop (tagged mdvlcraft_zone_marker) is removed when it loads again.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Zones {
    private static final String MARKER = "mdvlcraft_zone_marker";
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Set<UUID> LIVE_MARKERS = new HashSet<>();

    public static final class Zone {
        public final ServerLevel level;
        public final Vec3 center;
        public final LivingEntity caster;
        public final float power;
        private final long start;
        private final long end;
        private final int interval;
        private final Consumer<Zone> pulse;
        private final Runnable onEnd;
        @Nullable
        private final Entity marker;
        private boolean stopped;

        private Zone(ServerLevel level, Vec3 center, LivingEntity caster, float power, int duration, int interval, Consumer<Zone> pulse,
                     Runnable onEnd, @Nullable Entity marker) {
            this.level = level;
            this.center = center;
            this.caster = caster;
            this.power = power;
            this.start = level.getGameTime();
            this.end = this.start + duration;
            this.interval = interval;
            this.pulse = pulse;
            this.onEnd = onEnd;
            this.marker = marker;
        }

        /** Ends the zone at the end of this tick (a trap that has sprung). */
        public void stop() {
            this.stopped = true;
        }

        /** Ticks since the zone started. */
        public long age() {
            return this.level.getGameTime() - this.start;
        }
    }

    private Zones() {
    }

    /** Starts a zone; `marker` (may be null) is tagged and removed when the zone ends. */
    public static void start(ServerLevel level, Vec3 center, LivingEntity caster, float power, int duration, int interval, Consumer<Zone> pulse,
                             Runnable onEnd, @Nullable Entity marker) {
        if (marker != null) {
            marker.addTag(MARKER);
            LIVE_MARKERS.add(marker.getUUID());
        }
        ZONES.add(new Zone(level, center, caster, power, duration, interval, pulse, onEnd, marker));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent event) {
        if (event.phase != Phase.END || ZONES.isEmpty()) {
            return;
        }

        for (Zone zone : List.copyOf(ZONES)) {
            long now = zone.level.getGameTime();
            if (zone.stopped || now >= zone.end || !zone.caster.isAlive() && zone.caster.isRemoved()) {
                finish(zone);
            } else if ((now - zone.start) % zone.interval == 0) {
                zone.pulse.accept(zone);
            }
        }
    }

    private static void finish(Zone zone) {
        ZONES.remove(zone);
        if (zone.marker != null) {
            LIVE_MARKERS.remove(zone.marker.getUUID());
            zone.marker.discard();
        }
        zone.onEnd.run();
    }

    @SubscribeEvent
    public static void onMarkerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getTags().contains(MARKER) && !LIVE_MARKERS.contains(event.getEntity().getUUID())) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        List.copyOf(ZONES).forEach(Zones::finish);
    }
}
