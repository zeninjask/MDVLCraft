package com.mdvlcraft.binder.ability;

import cursedfate.entity.DomainExpansionEntityEntity;
import cursedfate.network.CursedfateModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Follows a binder player's Malevolent Shrine from cast to close.
 *
 * Casting it starts Cursed Fate's 2 s wind-up; the domain appears when that ends: for the closed shrine a domain
 * entity that raises the black barrier dome (and puts the terrain back when it closes), for the open shrine the shrine
 * entity. Cursed Fate then drains the owner's cursed energy by the domain's cost every second and closes it below 50,
 * but binder players pay with mana and hold only 100 cursed energy, so the domain would collapse after one second.
 * While the domain is younger than {@link #MAX_TICKS} the owner's cursed energy is topped up before each drain; after
 * that Cursed Fate closes it (with its usual burnout). Casting a Shrine again closes it earlier: the open shrine through
 * Cursed Fate's own toggle, the closed one through Cursed Fate's force-stop flag on the domain entity.
 */
final class DomainUpkeep {
    static final int MAX_TICKS = 20 * 20;
    /** Cursed Fate's wind-up is 40 ticks; give up on a domain that has not appeared well after that. */
    private static final int WIND_UP_TICKS = 100;
    /** The barrier dome's radius; Cursed Fate keeps a domain only while its owner is inside. */
    private static final double DOMAIN_RADIUS = 27;
    private static final ResourceLocation OPEN_SHRINE = ResourceLocation.fromNamespaceAndPath("cursedfate", "shrine");

    /** openedTick is negative while Cursed Fate is still winding up. */
    private record Domain(Technique shrine, long castTick, long openedTick) {
    }

    private static final Map<UUID, Domain> DOMAINS = new HashMap<>();

    private DomainUpkeep() {
    }

    /** A Shrine was cast and Cursed Fate started winding up a domain. */
    static void windingUp(ServerPlayer player, Technique shrine) {
        DOMAINS.put(player.getUUID(), new Domain(shrine, player.level().getGameTime(), -1));
    }

    static boolean isWindingUp(ServerPlayer player) {
        Domain domain = DOMAINS.get(player.getUUID());
        return domain != null && domain.openedTick() < 0;
    }

    /** Closes the player's closed domain if they are in one; true if it did. Closing is free. */
    static boolean closeClosedDomain(ServerPlayer player) {
        List<DomainExpansionEntityEntity> domains = ownedClosedDomains(player);
        if (domains.isEmpty()) {
            return false;
        }
        // Cursed Fate checks the flag on the domain's next one-second tick and closes it like an expired domain.
        domains.forEach(domain -> domain.getPersistentData().putBoolean("forcestopdomain", true));
        DOMAINS.remove(player.getUUID());
        return true;
    }

    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Domain>> entries = DOMAINS.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Domain> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
                continue;
            }
            Domain domain = entry.getValue();
            long now = player.level().getGameTime();
            boolean up = domain.shrine() == Technique.MALEVOLENT_SHRINE ? !ownedClosedDomains(player).isEmpty() : ownsOpenShrine(player);
            if (domain.openedTick() < 0) {
                if (up) {
                    entry.setValue(new Domain(domain.shrine(), domain.castTick(), now));
                } else if (now - domain.castTick() > WIND_UP_TICKS) {
                    entries.remove();
                }
                continue;
            }
            if (!up || now - domain.openedTick() >= MAX_TICKS) {
                entries.remove();
                continue;
            }
            CursedfateModVariables.PlayerVariables variables = TechniqueRunner.variables(player);
            // Enough to stay above Cursed Fate's 50-energy floor after its next drain.
            double needed = player.getPersistentData().getDouble("DomainCECost") + variables.CursedEnegryMax;
            if (variables.CursedEnegry < needed) {
                variables.CursedEnegry = needed;
                variables.syncPlayerVariables(player);
            }
        }
    }

    /** Cursed Fate marks a closed domain's entity with its owner's UUID. */
    private static List<DomainExpansionEntityEntity> ownedClosedDomains(ServerPlayer player) {
        String owner = player.getStringUUID();
        return player.level().getEntitiesOfClass(DomainExpansionEntityEntity.class, player.getBoundingBox().inflate(DOMAIN_RADIUS),
                entity -> owner.equals(entity.getPersistentData().getString("CurrentDomainOwner")));
    }

    /** Cursed Fate marks the open Shrine with its owner's UUID and only keeps it while the owner is within 27 blocks. */
    private static boolean ownsOpenShrine(ServerPlayer player) {
        EntityType<?> shrineType = ForgeRegistries.ENTITY_TYPES.getValue(OPEN_SHRINE);
        if (shrineType == null || !OPEN_SHRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(shrineType))) {
            throw new IllegalStateException("Cursed Fate entity " + OPEN_SHRINE + " is not registered");
        }
        String owner = player.getStringUUID();
        return !player.level().getEntities((Entity) null, player.getBoundingBox().inflate(DOMAIN_RADIUS),
                entity -> entity.getType() == shrineType && owner.equals(entity.getPersistentData().getString("PlayerUUID"))).isEmpty();
    }
}
