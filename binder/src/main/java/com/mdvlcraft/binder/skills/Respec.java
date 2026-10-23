package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.music.Jingle;
import com.mdvlcraft.binder.music.Jingles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.OpenArchetypePickPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill.State;

/**
 * The 19 archetypes and the player's vocation. Two archetypes picked on the Archetypes page (ArchetypeTabs) make a
 * vocation: one skill tab, mdvlcraft:voc_<a>_<b> (skills/VocationParts), holding both archetypes' base trees and the
 * pair's own skills. A respec picks both archetypes again and starts the new vocation tab empty at three quarters of the
 * old level. A reset keeps the vocation and its level and gives back every point spent on it.
 */
public final class Respec {
    /** In the design's order (tools/vocations/v2_base.ORDER); a vocation tab is named with the earlier one first. */
    public static final List<String> ARCHETYPES = List.of(
        "guardian", "brute", "samurai", "berserker", "monk",
        "blaze", "frost", "water", "lightning", "holy", "warlock", "necromancer", "druid", "jade", "spirit",
        "assassin", "thief", "stalker", "wanderer"
    );
    /** Each archetype's class: the class node it hangs from on the Archetypes page. */
    private static final Map<String, String> CLASS = Map.ofEntries(
        Map.entry("guardian", "warrior"), Map.entry("brute", "warrior"), Map.entry("samurai", "warrior"),
        Map.entry("berserker", "warrior"), Map.entry("monk", "warrior"),
        Map.entry("blaze", "mage"), Map.entry("frost", "mage"), Map.entry("water", "mage"), Map.entry("lightning", "mage"),
        Map.entry("holy", "mage"), Map.entry("warlock", "mage"), Map.entry("necromancer", "mage"), Map.entry("druid", "mage"),
        Map.entry("jade", "mage"), Map.entry("spirit", "mage"),
        Map.entry("assassin", "rogue"), Map.entry("thief", "rogue"), Map.entry("stalker", "rogue"), Map.entry("wanderer", "rogue")
    );

    private Respec() {
    }

    /** Set by /mdvl respec: the player's next respec keeps their whole level. In the persisted tag, so a death keeps it. */
    private static final String FREE = "mdvlcraft_free_respec";

    /** Opens the archetype picker for a free respec; false if the player has no vocation yet. */
    public static boolean offerFree(ServerPlayer player) {
        if (picked(player).size() != 2) {
            return false;
        }

        persisted(player).putBoolean(FREE, true);
        int level = vocation(player).flatMap(tab -> tab.getExperience().map(e -> e.getLevel(player))).orElse(0);
        BinderNetwork.sendTo(player, new OpenArchetypePickPacket(level, true));
        return true;
    }

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }

        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int keptLevel(int level) {
        return level * 3 / 4;
    }

    /**
     * Picks both archetypes again (any two, the same pair included): every node of the old vocation tab is gone, the new
     * pair's tab starts empty at three quarters of the old level, and all its points are spent afresh.
     */
    public static void respec(ServerPlayer player, String first, String second) {
        List<String> picked = picked(player);
        if (picked.size() != 2 || first.equals(second) || !ARCHETYPES.contains(first) || !ARCHETYPES.contains(second)) {
            return;
        }

        Category oldTab = vocation(player).orElseThrow(() -> new IllegalStateException("Two archetypes but no vocation tab"));
        int oldLevel = oldTab.getExperience().map(e -> e.getLevel(player)).orElse(0);
        boolean free = persisted(player).getBoolean(FREE);
        persisted(player).remove(FREE);
        int level = free ? oldLevel : keptLevel(oldLevel);
        String oldName = vocationName(player);
        oldTab.erase(player);
        picked.forEach(a -> node(a).lock(player));
        Category newTab = tab(first, second);
        newTab.erase(player);
        node(first).unlock(player);
        node(second).unlock(player);   // ArchetypeTabs opens the new pair's tab
        newTab.getExperience().ifPresent(e -> e.setLevel(player, level));
        AbilityActions.dropUngranted(player);
        player.displayClientMessage(Component.translatable("message.mdvlcraft.respec.changed", oldName, vocationName(player), level), false);
        Jingles.send(player, Jingle.JOB_CHANGE);
        newTab.openScreen(player);
    }

    /** Unlearns every skill of the player's vocation tab and refunds all its points; the archetypes and level stay (the client opens the tab). */
    public static void reset(ServerPlayer player) {
        Optional<Category> tab = vocation(player);
        if (tab.isEmpty()) {
            return;
        }

        tab.get().resetSkills(player);
        AbilityActions.dropUngranted(player);
        player.displayClientMessage(
            Component.translatable("message.mdvlcraft.respec.reset", vocationName(player), tab.get().getPointsLeft(player)), false
        );
    }

    public static boolean isPicked(ServerPlayer player, String archetype) {
        return ARCHETYPES.contains(archetype) && node(archetype).getState(player) == State.UNLOCKED;
    }

    /** The picked archetypes, in ARCHETYPES order. */
    public static List<String> picked(ServerPlayer player) {
        return ARCHETYPES.stream().filter(a -> isPicked(player, a)).toList();
    }

    public static String classOf(String archetype) {
        String type = CLASS.get(archetype);
        if (type == null) {
            throw new IllegalArgumentException("Not an archetype: " + archetype);
        }

        return type;
    }

    /** The tab of two archetypes (in either order). */
    public static Category tab(String first, String second) {
        String path = VocationParts.tabOf(first, second)
            .orElseThrow(() -> new IllegalStateException("No vocation for " + first + " and " + second));
        return SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", path))
            .orElseThrow(() -> new IllegalStateException("Skill tab mdvlcraft:" + path + " is not loaded"));
    }

    /** The player's vocation tab, when they have picked two archetypes. */
    public static Optional<Category> vocation(ServerPlayer player) {
        List<String> picked = picked(player);
        return picked.size() == 2 ? Optional.of(tab(picked.get(0), picked.get(1))) : Optional.empty();
    }

    /** The vocation's name, the one archetype's name, or "" with none picked. */
    public static String vocationName(ServerPlayer player) {
        List<String> picked = picked(player);
        return switch (picked.size()) {
            case 0 -> "";
            case 1 -> title(picked.get(0));
            default -> VocationParts.vocation(VocationParts.tabOf(picked.get(0), picked.get(1)).orElseThrow())
                .orElseThrow().get("name").getAsString();
        };
    }

    public static String title(String archetype) {
        return Character.toUpperCase(archetype.charAt(0)) + archetype.substring(1);
    }

    public static Skill node(String archetype) {
        return SkillsAPI.getCategory(ArchetypeTabs.ARCHETYPES)
            .flatMap(c -> c.getSkill(archetype))
            .orElseThrow(() -> new IllegalStateException("Archetypes page has no " + archetype + " node"));
    }
}
