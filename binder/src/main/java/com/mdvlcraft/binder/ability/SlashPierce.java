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
    public static final float SCALE = 0.5F;
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

    /** Whether this hit is a slash: Cursed Fate damage from a player who just slashed or whose shrine stands. */
    public static boolean slashHit(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player)
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
