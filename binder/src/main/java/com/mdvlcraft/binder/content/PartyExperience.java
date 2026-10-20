package com.mdvlcraft.binder.content;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Skill experience from a kill made in a party is shared, by the killer's Experience Share setting (ExpShare). Off: the
 * killer keeps 75% and the other 25% is split evenly between the rest of the party. On: the whole party, killer included,
 * splits it evenly. Party members count wherever they are, as long as they are online.
 */
public final class PartyExperience {
    private static final double PARTY_CUT = 0.25;
    /** The mob whose kill experience is being handed out, until its first hand-out is shared. */
    private static final ThreadLocal<LivingEntity> DYING = new ThreadLocal<>();

    private PartyExperience() {
    }

    public static void dying(LivingEntity victim) {
        DYING.set(victim);
    }

    public static void died() {
        DYING.remove();
    }

    /**
     * Takes the mob being killed, if a kill hand-out is under way and hasn't been shared yet. Later hand-outs for the
     * same death (Pufferfish's damage-share pass) go through unshared.
     */
    public static LivingEntity takeDying() {
        LivingEntity victim = DYING.get();
        DYING.remove();
        return victim;
    }

    /** Each player's share of the killer's experience; just the killer at 1 when they aren't in a party. */
    public static Map<ServerPlayer, Double> shares(ServerPlayer killer) {
        List<ServerPlayer> party = killer.server.getPlayerList().getPlayers().stream()
            .filter(other -> other != killer && AllianceAPI.areAllied(killer, other))
            .toList();
        Map<ServerPlayer, Double> shares = new HashMap<>();
        if (party.isEmpty()) {
            shares.put(killer, 1.0);
            return shares;
        }

        if (ExpShare.isOn(killer)) {
            double even = 1.0 / (party.size() + 1);
            shares.put(killer, even);
            party.forEach(member -> shares.put(member, even));
            return shares;
        }

        shares.put(killer, 1.0 - PARTY_CUT);
        party.forEach(member -> shares.put(member, PARTY_CUT / party.size()));
        return shares;
    }
}
