package com.mdvlcraft.binder.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The player's ability wheel: {@link #SLOTS} slots holding ability IDs, saved in the player's persisted data so it
 * survives death. Slots keep their ability when it stops being granted, so it returns if the skill is unlocked again.
 */
public final class Loadout {
    public static final int SLOTS = 8;
    private static final String KEY = "mdvlcraft:loadout";
    private static final String EMPTY = "";

    private Loadout() {
    }

    public static List<Optional<ResourceLocation>> get(Player player) {
        ListTag tag = data(player).getList(KEY, Tag.TAG_STRING);
        List<Optional<ResourceLocation>> slots = new ArrayList<>(Collections.nCopies(SLOTS, Optional.empty()));
        for (int i = 0; i < Math.min(SLOTS, tag.size()); i++) {
            String id = tag.getString(i);
            slots.set(i, id.equals(EMPTY) ? Optional.empty() : Optional.of(ResourceLocation.parse(id)));
        }
        return slots;
    }

    public static void set(ServerPlayer player, int slot, Optional<ResourceLocation> ability) {
        List<Optional<ResourceLocation>> slots = get(player);
        slots.set(slot, ability);
        ListTag tag = new ListTag();
        slots.forEach(id -> tag.add(StringTag.valueOf(id.map(ResourceLocation::toString).orElse(EMPTY))));
        data(player).put(KEY, tag);
        AbilityGrants.markDirty(player);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
