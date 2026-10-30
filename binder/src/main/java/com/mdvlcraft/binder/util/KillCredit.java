package com.mdvlcraft.binder.util;

import com.mdvlcraft.binder.modified.Summons;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraftforge.common.util.FakePlayer;

/**
 * The player a kill counts for: whoever landed the blow; else the owner of the summon, pet or familiar that did; else the
 * player who last hurt the victim (fire, bleeding, a fall after a hit...). Null when no player had a hand in it.
 */
public final class KillCredit {
    private KillCredit() {
    }

    @Nullable
    public static ServerPlayer of(LivingEntity dead, DamageSource source) {
        Entity attacker = source.getEntity();
        ServerPlayer player = attacker instanceof ServerPlayer direct ? direct : null;
        if (player == null && attacker != null) {
            Entity owner = Summons.owner(attacker);
            if (owner == null && attacker instanceof OwnableEntity ownable) {
                owner = ownable.getOwner();
            }
            player = owner instanceof ServerPlayer summoner ? summoner : null;
        }
        if (player == null && dead.getKillCredit() instanceof ServerPlayer last) {
            player = last;
        }
        return player instanceof FakePlayer ? null : player;
    }
}
