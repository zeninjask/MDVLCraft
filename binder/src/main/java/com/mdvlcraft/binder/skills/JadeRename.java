package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.mixin.puffish.CategoryDataAccessor;
import com.mdvlcraft.binder.mixin.puffish.PlayerDataAccessor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.ServerData;

/**
 * 2.0.14 renamed the Spirit archetype to Jade and added a new Spirit archetype in its place. Jade sits where Spirit was in
 * Respec.ARCHETYPES, so an old Spirit vocation tab voc_..spirit.. is now voc_..jade.., its base nodes spirit__* are
 * jade__*, and the Spirit pick on the Archetypes page is the Jade pick. This moves a player's saved data over once, on
 * their first join after the update and before Pufferfish syncs and applies it (HIGHEST priority); a flag in the
 * player's persistent data marks it done. The two Jade base nodes that changed (Soul Burst and Soul Catalyst, now in the
 * new Spirit base) match no node any more, so their points are free to spend again.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class JadeRename {
    private static final String DONE = "mdvlcraft_jade_rename";
    private static final String OLD = "spirit";
    private static final String NEW = "jade";

    private JadeRename() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(DONE)) {
            return;
        }

        Map<ResourceLocation, CategoryData> saved = ((PlayerDataAccessor)ServerData.getOrCreate(player.server).getPlayerData(player)).mdvlcraft$categories();
        CategoryData picks = saved.get(ArchetypeTabs.ARCHETYPES);
        if (picks != null) {
            Set<String> unlocked = ((CategoryDataAccessor)picks).mdvlcraft$unlockedSkills();
            if (unlocked.remove(OLD)) {
                unlocked.add(NEW);
            }
        }

        for (ResourceLocation id : List.copyOf(saved.keySet())) {
            String newPath = renamed(id);
            if (newPath != null) {
                CategoryData tab = saved.remove(id);
                Set<String> unlocked = ((CategoryDataAccessor)tab).mdvlcraft$unlockedSkills();
                List<String> old = unlocked.stream().filter(s -> s.startsWith(OLD + "__")).toList();
                old.forEach(s -> {
                    unlocked.remove(s);
                    unlocked.add(NEW + s.substring(OLD.length()));
                });
                saved.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), newPath), tab);
            }
        }

        data.putBoolean(DONE, true);
    }

    /** The new id of an old Spirit vocation tab (mdvlcraft:voc_<a>_spirit or voc_spirit_<b>), or null. */
    private static String renamed(ResourceLocation id) {
        if (!id.getNamespace().equals("mdvlcraft") || !id.getPath().startsWith("voc_")) {
            return null;
        }

        String[] pair = id.getPath().substring("voc_".length()).split("_");
        if (pair.length != 2 || !(pair[0].equals(OLD) || pair[1].equals(OLD))) {
            return null;
        }

        return "voc_" + (pair[0].equals(OLD) ? NEW : pair[0]) + "_" + (pair[1].equals(OLD) ? NEW : pair[1]);
    }
}
