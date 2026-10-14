package com.mdvlcraft.binder.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;

/**
 * The experience bars under the party windows, Pokemon style: a gain shows the bar, which fills from where the player
 * was to where they are now (emptying and filling again for each level gained) with "+N" counting up beneath it. A
 * gain while it is still filling adds on: the bar carries on to the new point and N grows. The bar goes two seconds
 * after it stops.
 *
 * Progress is the level plus the fraction into it, so a fill across levels is one moving number.
 */
final class ExpBars {
    /** Bars a second, at least; a big gain fills faster so it takes at most MAX_FILL. */
    private static final double SPEED = 0.6;
    private static final double MAX_FILL = 2.5;
    private static final long LINGER_MS = 2000L;
    private static final Map<UUID, Bar> BARS = new HashMap<>();

    private ExpBars() {
    }

    static void gain(UUID player, double from, double to, int gained) {
        long now = Util.getMillis();
        Bar bar = BARS.get(player);
        if (bar == null || bar.finished(now)) {
            BARS.put(player, new Bar(from, to, gained, now));
        } else {
            bar.extend(to, gained, now);
        }
    }

    /** The bar to draw for this player, or null when none is showing. */
    static Bar get(UUID player) {
        Bar bar = BARS.get(player);
        long now = Util.getMillis();
        if (bar != null && bar.finished(now)) {
            BARS.remove(player);
            return null;
        }

        if (bar != null) {
            bar.advance(now);
        }

        return bar;
    }

    static void reset() {
        BARS.clear();
    }

    static final class Bar {
        private double shown;
        private double target;
        private double speed;
        private int total;
        private double counted;
        private long last;
        private long stoppedAt;

        private Bar(double from, double to, int gained, long now) {
            this.shown = from;
            this.target = from;
            this.last = now;
            this.extend(to, gained, now);
        }

        private void extend(double to, int gained, long now) {
            this.target = Math.max(this.target, to);
            this.total += gained;
            this.speed = Math.max(SPEED, (this.target - this.shown) / MAX_FILL);
            this.stoppedAt = -1L;
        }

        private void advance(long now) {
            double seconds = (now - this.last) / 1000.0;
            this.last = now;
            if (this.shown < this.target) {
                double before = this.target - this.shown;
                this.shown = Math.min(this.target, this.shown + this.speed * seconds);
                this.counted += (this.total - this.counted) * Math.min(1.0, (before - (this.target - this.shown)) / before);
            }

            if (this.shown >= this.target && this.stoppedAt < 0L) {
                this.counted = this.total;
                this.stoppedAt = now;
            }
        }

        private boolean finished(long now) {
            return this.stoppedAt >= 0L && now - this.stoppedAt > LINGER_MS;
        }

        /** How full the bar is, 0 to 1, in the level it is passing through. */
        double fill() {
            return this.shown - Math.floor(this.shown);
        }

        /** The level the bar is passing through. */
        int level() {
            return (int)Math.floor(this.shown);
        }

        /** The experience shown beneath the bar, counting up to the total gained. */
        int counted() {
            return (int)Math.round(this.counted);
        }
    }
}
