package com.mdvlcraft.binder.ability;

import cursedfate.network.CursedfateModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Follows a binder player's Malevolent Shrine from cast to close.
 *
 * Casting it starts Cursed Fate's 2 s wind-up; the domain entity appears when that ends. Cursed Fate then drains the
 * owner's cursed energy by the domain's cost every second and closes it below 50, but binder players pay with mana
 * and hold only 100 cursed energy, so the domain would collapse after one second. While the domain is younger than
 * {@link #MAX_TICKS} the owner's cursed energy is topped up before each drain; after that Cursed Fate closes it (with
 * its usual burnout). Casting the Shrine again closes it earlier, through Cursed Fate's own toggle.
 */
final class DomainUpkeep {
    static final int MAX_TICKS = 20 * 20;
    /** Cursed Fate's wind-up is 40 ticks; give up on a domain that has not appeared well after that. */
    private static final int WIND_UP_TICKS = 100;
    private static final ResourceLocation OPEN_SHRINE = ResourceLocation.fromNamespaceAndPath("cursedfate", "shrine");

    /** openedTick is negative while Cursed Fate is still winding up. */
    private record Domain(long castTick, long openedTick) {
    }

    private static final Map<UUID, Domain> DOMAINS = new HashMap<>();

    private DomainUpkeep() {
    }

    /** The Shrine was cast and Cursed Fate started winding up a domain. */
    static void windingUp(ServerPlayer player) {
        DOMAINS.put(player.getUUID(), new Domain(player.level().getGameTime(), -1));
    }

    static boolean isWindingUp(ServerPlayer player) {
        Domain domain = DOMAINS.get(player.getUUID());
        return domain != null && domain.openedTick() < 0;
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
            boolean open = ownsOpenShrine(player);
            if (domain.openedTick() < 0) {
                if (open) {
                    entry.setValue(new Domain(domain.castTick(), now));
                } else if (now - domain.castTick() > WIND_UP_TICKS) {
                    entries.remove();
                }
                continue;
            }
            if (!open || now - domain.openedTick() >= MAX_TICKS) {
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

    /** Cursed Fate marks the open Shrine with its owner's UUID and only keeps it while the owner is within 27 blocks. */
    private static boolean ownsOpenShrine(ServerPlayer player) {
        EntityType<?> shrineType = ForgeRegistries.ENTITY_TYPES.getValue(OPEN_SHRINE);
        if (shrineType == null || !OPEN_SHRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(shrineType))) {
            throw new IllegalStateException("Cursed Fate entity " + OPEN_SHRINE + " is not registered");
        }
        String owner = player.getStringUUID();
        return !player.level().getEntities((Entity) null, player.getBoundingBox().inflate(27),
                entity -> entity.getType() == shrineType && owner.equals(entity.getPersistentData().getString("PlayerUUID"))).isEmpty();
    }
}
