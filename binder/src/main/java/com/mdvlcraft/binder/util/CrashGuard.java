package com.mdvlcraft.binder.util;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;
import org.slf4j.Logger;

/**
 * Catches errors where Minecraft would turn them into a crash (an entity, player or block entity tick, an entity's
 * renderer, a HUD overlay; see the mixin/guard package) and logs them instead, so they can be fixed from the logs. Every
 * line starts with "[CrashGuard]". The first time an error happens at a place its full stack trace is logged; after that
 * only every REPEAT_EVERY-th repeat is noted, with the count. An entity that keeps failing to tick is removed (never a
 * player), since an error every tick would otherwise flood the log and stall the server.
 */
public final class CrashGuard {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String TAG = "[CrashGuard] ";
    private static final int REPEAT_EVERY = 200;
    /** Ticks in a row an entity may fail before it is removed. */
    private static final int ENTITY_FAILURES = 100;
    private static final Map<String, AtomicInteger> SEEN = new ConcurrentHashMap<>();
    private static final Map<Integer, AtomicInteger> FAILING = new ConcurrentHashMap<>();

    private CrashGuard() {
    }

    /** Logs an error that would have crashed the game at `where` (what was running, e.g. "entity tick"). */
    public static void caught(String where, String detail, Throwable error) {
        StackTraceElement[] trace = error.getStackTrace();
        String key = where + "|" + error.getClass().getName() + "|" + (trace.length > 0 ? trace[0] : "");
        int count = SEEN.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (count == 1) {
            LOG.error(TAG + "Prevented a crash in {} ({}). Please report this log. Error:", where, detail, error);
        } else if (count % REPEAT_EVERY == 0) {
            LOG.error(TAG + "The {} error in {} ({}) has now happened {} times: {}", error.getClass().getSimpleName(), where, detail, count, error.toString());
        }
    }

    /** Whether an error is one to catch: not the JVM running out of memory or being shut down. */
    public static boolean catchable(Throwable error) {
        return !(error instanceof VirtualMachineError) || error instanceof StackOverflowError;
    }

    public static void entityTickFailed(Entity entity, Throwable error) {
        caught("entity tick", describe(entity), error);
        if (entity instanceof Player) {
            return;
        }
        int failures = FAILING.computeIfAbsent(entity.getId(), id -> new AtomicInteger()).incrementAndGet();
        if (failures >= ENTITY_FAILURES) {
            FAILING.remove(entity.getId());
            LOG.error(TAG + "Removed {} after {} failed ticks in a row, so it can't stall the game.", describe(entity), failures);
            entity.discard();
        }
    }

    public static void entityTicked(Entity entity) {
        if (!FAILING.isEmpty()) {
            FAILING.remove(entity.getId());
        }
    }

    /** The UUID written as `text`, or null (logged) if it isn't one, e.g. from damaged saved data. */
    @Nullable
    public static UUID uuid(String text, String where) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException error) {
            caught(where, "bad UUID '" + text + "'", error);
            return null;
        }
    }

    public static String describe(Entity entity) {
        if (entity == null) {
            return "no entity";
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " '" + entity.getName().getString() + "' #" + entity.getId()
            + " at " + entity.blockPosition().toShortString() + " in " + entity.level().dimension().location();
    }
}
