package com.mdvlcraft.binder.integration;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Parties travel to boss rooms together. When a player goes from an ordinary dimension into a boss dimension (Cataclysm
 * Dimension's eyes, the Mimic invitation, Arachne's and Leonidas's rooms, Wraithon's sanctum, BossArena's sigils), every
 * online party member who is not already in a boss dimension follows them to the same spot. When a player leaves a boss
 * dimension for an ordinary one (a Return Crystal, an arena return, the mod's own way out), the party members still in that
 * boss dimension go back too, each to where they stood before going in.
 */
public final class PartyTravel {
    private static final String ORIGIN = "mdvlcraft_party_origin";
    private static final Set<String> BOSS_NAMESPACES = Set.of(ReturnCrystal.DIMENSION_MOD);
    private static final Set<ResourceLocation> BOSS_DIMENSIONS = Set.of(
        ResourceLocation.fromNamespaceAndPath("epicfight_arachne", "arachne_nest"),
        ResourceLocation.fromNamespaceAndPath("epicfight_leonidas", "rockwind_ruins"),
        ResourceLocation.fromNamespaceAndPath("mimic", "the_lethean_sea"),
        ResourceLocation.fromNamespaceAndPath("wraithon", "sanctum_of_the_wraithon"),
        BossArena.ARENA.location());
    /** Set while this class moves party members, so their own moves don't pull the party again. */
    private static boolean moving;

    private PartyTravel() {
    }

    public static void register(IEventBus forgeBus) {
        forgeBus.addListener(PartyTravel::onTravel);
        forgeBus.addListener(PartyTravel::onChangedDimension);
    }

    public static boolean isBossDimension(ResourceKey<Level> dimension) {
        return BOSS_NAMESPACES.contains(dimension.location().getNamespace()) || BOSS_DIMENSIONS.contains(dimension.location());
    }

    /** Fired before the move, so the player still stands where they were. */
    private static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isBossDimension(event.getDimension()) && !isBossDimension(player.level().dimension())) {
            CompoundTag origin = new CompoundTag();
            origin.putString("dimension", player.level().dimension().location().toString());
            origin.putDouble("x", player.getX());
            origin.putDouble("y", player.getY());
            origin.putDouble("z", player.getZ());
            origin.putFloat("yaw", player.getYRot());
            origin.putFloat("pitch", player.getXRot());
            persisted(player).put(ORIGIN, origin);
        }
    }

    private static void onChangedDimension(PlayerChangedDimensionEvent event) {
        if (moving || !(event.getEntity() instanceof ServerPlayer leader)) {
            return;
        }
        boolean entering = isBossDimension(event.getTo()) && !isBossDimension(event.getFrom());
        boolean leaving = isBossDimension(event.getFrom()) && !isBossDimension(event.getTo());
        if (leaving) {
            persisted(leader).remove(ORIGIN);
        }
        if (!entering && !leaving) {
            return;
        }
        List<ServerPlayer> party = leader.server.getPlayerList().getPlayers().stream()
            .filter(other -> other != leader && other.isAlive() && AllianceAPI.areAllied(leader, other))
            .filter(other -> entering ? !isBossDimension(other.level().dimension()) : other.level().dimension() == event.getFrom())
            .toList();
        if (party.isEmpty()) {
            return;
        }
        // after the leader's move has finished
        leader.server.execute(() -> {
            moving = true;
            try {
                for (ServerPlayer member : party) {
                    if (entering) {
                        member.teleportTo(leader.serverLevel(), leader.getX(), leader.getY(), leader.getZ(), leader.getYRot(), leader.getXRot());
                    } else if (!returnHome(member)) {
                        member.teleportTo(leader.serverLevel(), leader.getX(), leader.getY(), leader.getZ(), member.getYRot(), member.getXRot());
                    }
                    member.fallDistance = 0.0F;
                }
            } finally {
                moving = false;
            }
        });
    }

    /** Sends the player back to where they stood before entering a boss dimension; false if that is not known. */
    public static boolean returnHome(ServerPlayer player) {
        CompoundTag origin = persisted(player).getCompound(ORIGIN);
        ServerLevel target = origin.isEmpty() ? null
            : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(origin.getString("dimension"))));
        if (target == null) {
            return false;
        }
        persisted(player).remove(ORIGIN);
        player.teleportTo(target, origin.getDouble("x"), origin.getDouble("y"), origin.getDouble("z"), origin.getFloat("yaw"), origin.getFloat("pitch"));
        player.fallDistance = 0.0F;
        return true;
    }

    @Nullable
    static CompoundTag origin(Player player) {
        CompoundTag origin = persisted(player).getCompound(ORIGIN);
        return origin.isEmpty() ? null : origin;
    }

    /** The persisted tag survives death; the next boss dimension entered overwrites the origin. */
    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
