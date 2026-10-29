package com.mdvlcraft.binder.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Slashing techniques pierce armour: armour blocks only {@link #SCALE} of what it would against Cursed Fate damage dealt by
 * a player within {@link #WINDOW} ticks of casting Dismantle, Cleave, Chop, Mince or Dicing Board, or while their Malevolent
 * Shrine stands (its cuts). Applied in mixin/attributeslib/ALCombatRulesMixin, after the pack's own armour rules.
 */
public final class SlashPierce {
    public static final float SCALE = 0.4F;   // 60% pierce (50% x 1.2)
    /** Shrine attacks deal this share of their damage. */
    public static final float SHRINE_DAMAGE = 0.85F * 0.75F;   // -15%, then -25%; Shrine Mastery nodes add back (BinderAttributes.SHRINE_DAMAGE)
    private static final ThreadLocal<Boolean> SEARING = ThreadLocal.withInitial(() -> false);
    /** How long a slash's cuts land after its cast (2 s). */
    private static final long WINDOW = 40L;
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private SlashPierce() {
    }

    static void noteCast(ServerPlayer player, Technique technique) {
        if (technique.isSlash() || technique == Technique.DICING_BOARD) {
            UNTIL.put(player.getUUID(), player.level().getGameTime() + WINDOW);
        }
    }

    /** Whether the hit being dealt now is one of Sear's (searing). */
    public static boolean searingNow() {
        return SEARING.get();
    }

    /** Runs a hit of Sear's (mixin/cursedfate/SearHitMixin): it isn't a shrine attack, even inside the chef's shrine. */
    public static <T> T searing(java.util.function.Supplier<T> hit) {
        SEARING.set(true);
        try {
            return hit.get();
        } finally {
            SEARING.set(false);
        }
    }

    /**
     * Whether this hit is a shrine attack: Cursed Fate damage from a player who just slashed or whose shrine stands
     * (not Sear's). Shrine attacks pierce armour (SCALE) and deal SHRINE_DAMAGE (combat/TechniqueScaling).
     */
    public static boolean slashHit(DamageSource source) {
        if (SEARING.get() || !(source.getEntity() instanceof ServerPlayer player)
            || !source.typeHolder().unwrapKey().map(key -> key.location().getNamespace().equals("cursedfate")).orElse(false)) {
            return false;
        }
        Long until = UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until || DomainUpkeep.isActive(player);
    }

    public static float scale(float damage, float after) {
        return damage - (damage - after) * SCALE;
    }
}
