package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.mixin.puffish.CategoryDataAccessor;
import com.mdvlcraft.binder.mixin.puffish.PlayerDataAccessor;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.OpenArchetypePickPacket;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.ServerData;

/**
 * Players from before the vocations rework (archetypes picked on the old Classes page, one tab per archetype) pick their
 * two new archetypes in a pop-up on joining (client/RespecScreen in pick mode). Their vocation tab then starts at the
 * highest level any old archetype tab had. Pufferfish keeps the old tabs' saved data although those tabs no longer load,
 * so the old picks and levels are read from it. Runs once per player: a flag in the player's persistent data marks it
 * done, and a player who closes the pop-up gets it again on their next join; picking on the Archetypes tab instead works
 * too (ArchetypeTabs applies the level when the vocation tab opens).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class LegacyMigration {
    private static final String DONE = "mdvlcraft_vocations";
    private static final String PENDING_LEVEL = "mdvlcraft_vocation_level";
    private static final ResourceLocation OLD_CLASSES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "classes");
    private static final List<String> OLD_ARCHETYPES = List.of(
        "knight", "samurai", "berzerker", "monk", "holy", "flame", "lightning", "necromancy", "ice", "water", "assassin",
        "phantom", "scout", "thief", "wanderer", "stalker", "druid", "cursed", "warlock", "bloodborn", "magehunter"
    );

    private LegacyMigration() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(DONE)) {
            return;
        }

        Map<ResourceLocation, CategoryData> saved = ((PlayerDataAccessor)ServerData.getOrCreate(player.server).getPlayerData(player)).mdvlcraft$categories();
        CategoryData oldClasses = saved.get(OLD_CLASSES);
        boolean pickedBefore = oldClasses != null
            && ((CategoryDataAccessor)oldClasses).mdvlcraft$unlockedSkills().stream().anyMatch(OLD_ARCHETYPES::contains);
        if (!pickedBefore || Respec.picked(player).size() == 2) {
            data.putBoolean(DONE, true);
            return;
        }

        int level = OLD_ARCHETYPES.stream()
            .map(id -> saved.get(ResourceLocation.fromNamespaceAndPath("mdvlcraft", id)))
            .filter(java.util.Objects::nonNull)
            .mapToInt(c -> oldLevel(c.getExperience()))
            .max()
            .orElse(0);
        data.putInt(PENDING_LEVEL, level);
        BinderNetwork.sendTo(player, new OpenArchetypePickPacket(level, false));
    }

    /** The old trees' level for this much earned experience (their curve: 25 + 5 x level to go from level to level + 1). */
    static int oldLevel(int experience) {
        int level = 0;
        int needed = 25;
        while (experience >= needed) {
            experience -= needed;
            level++;
            needed = 25 + level * 5;
        }

        return level;
    }

    /** The pop-up's answer: picks both archetypes; the vocation tab's level is set when it opens (applyPendingLevel). */
    public static void pick(ServerPlayer player, String first, String second) {
        if (!player.getPersistentData().contains(PENDING_LEVEL) || !Respec.picked(player).isEmpty() || first.equals(second)
            || !Respec.ARCHETYPES.contains(first) || !Respec.ARCHETYPES.contains(second)) {
            return;
        }

        Respec.node(first).unlock(player);
        Respec.node(second).unlock(player);
    }

    /** Gives a migrating player's new vocation tab their old level, once. Called when a vocation tab opens. */
    static void applyPendingLevel(ServerPlayer player, net.puffish.skillsmod.api.Category tab) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(PENDING_LEVEL)) {
            int level = data.getInt(PENDING_LEVEL);
            tab.getExperience().ifPresent(e -> e.setLevel(player, level));
            data.remove(PENDING_LEVEL);
            data.putBoolean(DONE, true);
        }
    }
}
