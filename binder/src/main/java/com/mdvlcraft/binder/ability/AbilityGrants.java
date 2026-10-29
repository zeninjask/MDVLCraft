package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AbilityGrants {
    private static final Map<UUID, Set<AbilityReward>> ACTIVE = new HashMap<>();
    private static final Set<UUID> DIRTY = new HashSet<>();
    /** Abilities given by /mdvl grant (testing), until the server stops. */
    private static final Map<UUID, Map<Ability, Integer>> GIVEN = new HashMap<>();

    private AbilityGrants() {
    }

    static void set(ServerPlayer player, AbilityReward reward, boolean active) {
        Set<AbilityReward> rewards = ACTIVE.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>());
        boolean changed = active ? rewards.add(reward) : rewards.remove(reward);
        if (rewards.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        }

        if (changed) {
            if (active && reward.ability instanceof SpellAbility spellAbility && spellAbility.spell().requiresLearning()) {
                MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spellAbility.spell());
            }

            if (!active && reward.ability == StanceAbility.INSTANCE && level(player, StanceAbility.INSTANCE) == 0) {
                Stances.deactivate(player);
            }

            DIRTY.add(player.getUUID());
        }
    }

    static void dispose(AbilityReward reward) {
        ACTIVE.forEach((uuid, rewards) -> {
            if (rewards.remove(reward)) {
                DIRTY.add(uuid);
            }
        });
        ACTIVE.values().removeIf(Set::isEmpty);
    }

    public static Map<Ability, Integer> of(ServerPlayer player) {
        Map<Ability, Integer> granted = new LinkedHashMap<>();

        for (AbilityReward reward : ACTIVE.getOrDefault(player.getUUID(), Set.of())) {
            granted.merge(reward.ability, reward.level, Math::max);
        }

        GIVEN.getOrDefault(player.getUUID(), Map.of()).forEach((ability, level) -> granted.merge(ability, level, Math::max));
        for (PlannedAbility ability : RaceAbilities.of(player)) {
            granted.putIfAbsent(ability, 1);
        }

        return granted;
    }

    public static int level(ServerPlayer player, Ability ability) {
        return of(player).getOrDefault(ability, 0);
    }

    /** Gives (level > 0) or takes back (0) an ability outside the skill trees, for testing (/mdvl grant). */
    public static void give(ServerPlayer player, Ability ability, int level) {
        Map<Ability, Integer> given = GIVEN.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        if (level > 0) {
            given.put(ability, level);
            if (ability instanceof SpellAbility spellAbility && spellAbility.spell().requiresLearning()) {
                MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spellAbility.spell());
            }
        } else {
            given.remove(ability);
        }
        DIRTY.add(player.getUUID());
    }

    /** Re-sends the player's abilities (something outside the skill trees changed what they are granted). */
    public static void refresh(ServerPlayer player) {
        DIRTY.add(player.getUUID());
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
