package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.doppelganger.DoppelgangerPatch;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Summons don't earn their summoner skill experience. Pufferfish credits a kill to the player its damage source names.
 * Summons such as Magic From the East's Ashigaru Squad hit with their summoner as the cause and themselves as the direct
 * attacker, and the doppelganger hits with its owner's own damage source, so their kills read as the player's. A kill
 * whose direct attacker is a living entity other than the credited player (projectiles still count), or one made while
 * a doppelganger's hit is applied, credits nobody.
 */
public final class SummonKills {
    /** The damage source of the death whose loot (and Pufferfish's kill experience) is being handed out. */
    private static final ThreadLocal<DamageSource> DYING_BY = new ThreadLocal<>();

    private SummonKills() {
    }

    public static void dying(DamageSource source) {
        DYING_BY.set(source);
    }

    public static void died() {
        DYING_BY.remove();
    }

    public static boolean creditBlocked() {
        if (DoppelgangerPatch.attacking()) {
            return true;
        }
        DamageSource source = DYING_BY.get();
        return source != null && source.getEntity() instanceof Player player
            && source.getDirectEntity() instanceof LivingEntity direct && direct != player;
    }
}
