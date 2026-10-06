package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which abilities each online player has, derived from their unlocked {@link AbilityReward}s. Pufferfish's Skills
 * re-applies rewards on join and clears them on leave, so this state is never saved. Several skills may grant the same
 * ability; the highest level wins. Changes are sent to the client once per tick.
 */
public final class AbilityGrants {
    private static final Map<UUID, Set<AbilityReward>> ACTIVE = new HashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();

    private AbilityGrants() {
    }

    static void set(ServerPlayer player, AbilityReward reward, boolean active) {
        Set<AbilityReward> rewards = ACTIVE.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
        boolean changed = active ? rewards.add(reward) : rewards.remove(reward);
        if (rewards.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        }
        if (!changed) {
            return;
        }
        if (active && reward.ability instanceof SpellAbility spellAbility && spellAbility.spell().requiresLearning()) {
            // Eldritch spells must be learned before Iron's lets anyone cast them.
            MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spellAbility.spell());
        }
        DIRTY.add(player.getUUID());
    }

    static void dispose(AbilityReward reward) {
        ACTIVE.forEach((uuid, rewards) -> {
            if (rewards.remove(reward)) {
                DIRTY.add(uuid);
            }
        });
        ACTIVE.values().removeIf(Set::isEmpty);
    }

    /** Granted abilities and their levels. */
    public static Map<Ability, Integer> of(ServerPlayer player) {
        Map<Ability, Integer> granted = new LinkedHashMap<>();
        for (AbilityReward reward : ACTIVE.getOrDefault(player.getUUID(), Set.of())) {
            granted.merge(reward.ability, reward.level, Math::max);
        }
        return granted;
    }

    /** The level the player has the ability at, or 0 when it is not granted. */
    public static int level(ServerPlayer player, Ability ability) {
        return of(player).getOrDefault(ability, 0);
    }

    static void markDirty(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    static void flush(MinecraftServer server) {
        for (UUID uuid : DIRTY) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                AbilitySync.send(player);
            }
        }
        DIRTY.clear();
    }
}
