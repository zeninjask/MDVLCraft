package com.mdvlcraft.binder.ability;

import cursedfate.entity.DomainExpansionEntityEntity;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Follows a binder player's Malevolent Shrine (closed or open) from cast to close. While it stands it costs
 * {@link #manaPerSecond} mana a second (10, closed or open) and Cursed Fate's cursed energy is kept topped up; it closes
 * when the owner sneak-casts it again ({@link #close}) or cannot pay the next second. The closed shrine's cooldown
 * restarts in full when it closes, however it closes.
 */
final class DomainUpkeep {
    static float manaPerSecond(Technique shrine) {
        return 10.0F;
    }
    private static final int WIND_UP_TICKS = 100;
    private static final double DOMAIN_RADIUS = 27.0;
    private static final ResourceLocation OPEN_SHRINE = ResourceLocation.fromNamespaceAndPath("cursedfate", "shrine");
    private static final Map<UUID, DomainUpkeep.Domain> DOMAINS = new HashMap<>();

    private DomainUpkeep() {
    }

    static void windingUp(ServerPlayer player, Technique shrine) {
        DOMAINS.put(player.getUUID(), new DomainUpkeep.Domain(shrine, player.level().getGameTime(), -1L));
    }

    static boolean isActive(ServerPlayer player) {
        return DOMAINS.containsKey(player.getUUID());
    }

    /** Closes the player's shrine: the closed dome through Cursed Fate's force-stop, the open shrine by letting its cursed energy run out. */
    static void close(ServerPlayer player) {
        DomainUpkeep.Domain domain = DOMAINS.remove(player.getUUID());
        if (domain != null) {
            shut(player, domain);
        }
    }

    private static void shut(ServerPlayer player, DomainUpkeep.Domain domain) {
        ended(player, domain);
        if (domain.shrine() == Technique.MALEVOLENT_SHRINE) {
            ownedClosedDomains(player).forEach(entity -> entity.getPersistentData().putBoolean("forcestopdomain", true));
        } else {
            PlayerVariables variables = TechniqueRunner.variables(player);
            variables.CursedEnegry = 0.0;
            variables.syncPlayerVariables(player);
        }
    }

    static void tick(MinecraftServer server) {
        Iterator<Entry<UUID, DomainUpkeep.Domain>> entries = DOMAINS.entrySet().iterator();

        while (entries.hasNext()) {
            Entry<UUID, DomainUpkeep.Domain> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
            } else {
                DomainUpkeep.Domain domain = entry.getValue();
                long now = player.level().getGameTime();
                boolean up = domain.shrine() == Technique.MALEVOLENT_SHRINE ? !ownedClosedDomains(player).isEmpty() : ownsOpenShrine(player);
                if (domain.openedTick() < 0L) {
                    if (up) {
                        entry.setValue(new DomainUpkeep.Domain(domain.shrine(), domain.castTick(), now));
                    } else if (now - domain.castTick() > 100L) {
                        entries.remove();
                    }
                } else if (up) {
                    if ((now - domain.openedTick()) % 20L == 0L && !pay(player, domain.shrine())) {
                        entries.remove();
                        shut(player, domain);
                        continue;
                    }

                    PlayerVariables variables = TechniqueRunner.variables(player);
                    double needed = player.getPersistentData().getDouble("DomainCECost") + variables.CursedEnegryMax;
                    if (variables.CursedEnegry < needed) {
                        variables.CursedEnegry = needed;
                        variables.syncPlayerVariables(player);
                    }
                } else {
                    entries.remove();
                    ended(player, domain);
                }
            }
        }
    }

    /** An opened closed shrine has ended: its cooldown starts over from now. */
    private static void ended(ServerPlayer player, DomainUpkeep.Domain domain) {
        if (domain.shrine() == Technique.MALEVOLENT_SHRINE && domain.openedTick() >= 0L) {
            TechniqueCooldowns.start(player, Technique.MALEVOLENT_SHRINE);
        }
    }

    /** Takes one second of upkeep; false when the player cannot pay it. */
    private static boolean pay(ServerPlayer player, Technique shrine) {
        float cost = manaPerSecond(shrine);
        if (player.isCreative()) {
            return true;
        }

        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < cost) {
            return false;
        }

        magic.setMana(magic.getMana() - cost);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    private static List<DomainExpansionEntityEntity> ownedClosedDomains(ServerPlayer player) {
        String owner = player.getStringUUID();
        return player.level()
            .getEntitiesOfClass(
                DomainExpansionEntityEntity.class,
                player.getBoundingBox().inflate(27.0),
                entity -> owner.equals(entity.getPersistentData().getString("CurrentDomainOwner"))
            );
    }

    private static boolean ownsOpenShrine(ServerPlayer player) {
        EntityType<?> shrineType = ForgeRegistries.ENTITY_TYPES.getValue(OPEN_SHRINE);
        if (shrineType != null && OPEN_SHRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(shrineType))) {
            String owner = player.getStringUUID();
            return !player.level()
                .getEntities(
                    (Entity)null,
                    player.getBoundingBox().inflate(27.0),
                    entity -> entity.getType() == shrineType && owner.equals(entity.getPersistentData().getString("PlayerUUID"))
                )
                .isEmpty();
        } else {
            throw new IllegalStateException("Cursed Fate entity " + OPEN_SHRINE + " is not registered");
        }
    }

    private record Domain(Technique shrine, long castTick, long openedTick) {
    }
}
