package com.mdvlcraft.binder.modified;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Timed states for Modified abilities that need no effect icon: a buff on the caster ("for 8 s your hits ...") or a
 * mark on a target, kept in the entity's persistent data as the game time it ends, with an optional count (stacks,
 * charges) and the marker's UUID. Event handlers ask has()/markedBy() to decide what to do.
 */
public final class Buffs {
    private static final String ROOT = "mdvlcraft_buffs";

    private Buffs() {
    }

    private static CompoundTag tag(LivingEntity entity, String key) {
        CompoundTag all = entity.getPersistentData().getCompound(ROOT);
        entity.getPersistentData().put(ROOT, all);
        CompoundTag one = all.getCompound(key);
        all.put(key, one);
        return one;
    }

    /** Gives `entity` the state `key` for `ticks` (replacing any running one; the count goes back to 0). */
    public static void give(LivingEntity entity, String key, int ticks) {
        CompoundTag one = tag(entity, key);
        one.putLong("Until", entity.level().getGameTime() + ticks);
        one.putInt("Count", 0);
        one.remove("By");
    }

    public static boolean has(LivingEntity entity, String key) {
        CompoundTag all = entity.getPersistentData().getCompound(ROOT);
        return all.contains(key) && all.getCompound(key).getLong("Until") > entity.level().getGameTime();
    }

    public static void clear(LivingEntity entity, String key) {
        entity.getPersistentData().getCompound(ROOT).remove(key);
    }

    /** Ticks left on the state, 0 if none. */
    public static int remaining(LivingEntity entity, String key) {
        return has(entity, key) ? (int)(tag(entity, key).getLong("Until") - entity.level().getGameTime()) : 0;
    }

    /** Lengthens a running state by `ticks`. */
    public static void extend(LivingEntity entity, String key, int ticks) {
        if (has(entity, key)) {
            CompoundTag one = tag(entity, key);
            one.putLong("Until", one.getLong("Until") + ticks);
        }
    }

    /** The count kept with a running state (stacks, charges); 0 if none. */
    public static int count(LivingEntity entity, String key) {
        return has(entity, key) ? tag(entity, key).getInt("Count") : 0;
    }

    /** Adds `n` to the count of a running state and returns the new count (0 if the state isn't running). */
    public static int add(LivingEntity entity, String key, int n) {
        if (!has(entity, key)) {
            return 0;
        }
        CompoundTag one = tag(entity, key);
        one.putInt("Count", one.getInt("Count") + n);
        return one.getInt("Count");
    }

    /** Marks `target` with `key` for `ticks`, remembering who marked it. */
    public static void mark(LivingEntity target, String key, LivingEntity by, int ticks) {
        give(target, key, ticks);
        tag(target, key).putUUID("By", by.getUUID());
    }

    /** Whoever put a running mark `key` on `target`, if it is still in the same level. */
    @Nullable
    public static LivingEntity markedBy(LivingEntity target, String key) {
        if (!has(target, key) || !(target.level() instanceof ServerLevel level)) {
            return null;
        }
        CompoundTag one = tag(target, key);
        if (!one.hasUUID("By")) {
            return null;
        }
        UUID by = one.getUUID("By");
        return level.getEntity(by) instanceof LivingEntity marker ? marker : null;
    }
}
