package com.mdvlcraft.binder.traits;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SyncTraitsPacket;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The traits each player has: up to MAX_SLOTS, kept in the player's persisted data (they survive death), shown on the
 * Status page (client/CharacterScreen, from SyncTraitsPacket). Nothing hands them out yet; TraitCommands sets them.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Traits {
    public static final int MAX_SLOTS = 2;
    static final String KEY = "mdvlcraft:traits";
    static final String SLOTS = "mdvlcraft:trait_slots";
    private static final Map<UUID, Set<Trait>> CACHE = new HashMap<>();
    /** The local player's traits and slots, on the client (SyncTraitsPacket). */
    private static List<Trait> clientTraits = List.of();
    private static int clientSlots = 1;

    private Traits() {
    }

    /** Whether the player has the trait (on the client, only the local player's are known). */
    public static boolean has(@Nullable Entity entity, Trait trait) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.level().isClientSide) {
            return player.isLocalPlayer() && clientTraits.contains(trait);
        }
        return CACHE.computeIfAbsent(player.getUUID(), id -> {
            Set<Trait> set = EnumSet.noneOf(Trait.class);
            set.addAll(get(player));
            return set;
        }).contains(trait);
    }

    static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** The player's traits by slot (an empty slot is skipped). */
    public static List<Trait> get(Player player) {
        List<Trait> traits = new ArrayList<>();
        ListTag list = data(player).getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Trait trait = Trait.byId(list.getString(i));
            if (trait != null) {
                traits.add(trait);
            }
        }
        return traits;
    }

    /** The trait in a slot (0-based), or null. */
    @Nullable
    public static Trait inSlot(Player player, int slot) {
        ListTag list = data(player).getList(KEY, Tag.TAG_STRING);
        return slot < list.size() ? Trait.byId(list.getString(slot)) : null;
    }

    public static int slots(Player player) {
        return Math.max(1, Math.min(MAX_SLOTS, data(player).getInt(SLOTS)));
    }

    public static void setSlots(ServerPlayer player, int slots) {
        data(player).putInt(SLOTS, slots);
        sync(player);
    }

    /** Puts a trait in a slot (null empties it) and tells the client. */
    public static void set(ServerPlayer player, int slot, @Nullable Trait trait) {
        ListTag list = data(player).getList(KEY, Tag.TAG_STRING);
        while (list.size() <= slot) {
            list.add(StringTag.valueOf(""));
        }
        list.set(slot, StringTag.valueOf(trait == null ? "" : trait.id()));
        data(player).put(KEY, list);
        CACHE.remove(player.getUUID());
        TraitWorld.refresh(player);
        sync(player);
    }

    static void sync(ServerPlayer player) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < slots(player); i++) {
            Trait trait = inSlot(player, i);
            ids.add(trait == null ? "" : trait.id());
        }
        BinderNetwork.sendTo(player, new SyncTraitsPacket(ids, slots(player)));
    }

    /** SyncTraitsPacket, on the client. */
    public static void setClient(List<String> ids, int slots) {
        List<Trait> traits = new ArrayList<>();
        ids.forEach(id -> {
            Trait trait = Trait.byId(id);
            if (trait != null) {
                traits.add(trait);
            }
        });
        clientTraits = List.copyOf(traits);
        clientSlots = slots;
    }

    public static List<Trait> clientTraits() {
        return clientTraits;
    }

    public static int clientSlots() {
        return clientSlots;
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CACHE.remove(player.getUUID());
            TraitWorld.refresh(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TraitWorld.refresh(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        CACHE.remove(event.getEntity().getUUID());
    }
}
