package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.music.Jingle;
import com.mdvlcraft.binder.music.Jingles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill.State;

public final class Respec {
    public static final List<String> ARCHETYPES = List.of(
        "knight",
        "samurai",
        "berzerker",
        "monk",
        "holy",
        "flame",
        "lightning",
        "necromancy",
        "ice",
        "water",
        "assassin",
        "phantom",
        "scout",
        "thief",
        "wanderer",
        "stalker",
        "druid",
        "cursed",
        "warlock",
        "bloodborn",
        "magehunter"
    );
    /** Each archetype's class: the class node it hangs from on the Classes page. */
    private static final Map<String, String> CLASS = Map.ofEntries(
        Map.entry("knight", "warrior"),
        Map.entry("samurai", "warrior"),
        Map.entry("berzerker", "warrior"),
        Map.entry("monk", "warrior"),
        Map.entry("magehunter", "warrior"),
        Map.entry("holy", "mage"),
        Map.entry("flame", "mage"),
        Map.entry("lightning", "mage"),
        Map.entry("necromancy", "mage"),
        Map.entry("ice", "mage"),
        Map.entry("water", "mage"),
        Map.entry("warlock", "mage"),
        Map.entry("assassin", "rogue"),
        Map.entry("phantom", "rogue"),
        Map.entry("scout", "rogue"),
        Map.entry("thief", "rogue"),
        Map.entry("wanderer", "ranger"),
        Map.entry("stalker", "ranger"),
        Map.entry("druid", "ranger"),
        Map.entry("cursed", "ranger"),
        Map.entry("bloodborn", "ranger")
    );

    private Respec() {
    }

    public static int keptLevel(int level) {
        return level * 3 / 4;
    }

    public static void reset(ServerPlayer player, String archetype) {
        if (isPicked(player, archetype)) {
            tab(archetype).ifPresent(tab -> {
                tab.resetSkills(player);
                AbilityActions.dropUngranted(player);
                player.displayClientMessage(Component.translatable("message.mdvlcraft.respec.reset", title(archetype)), false);
                Jingles.send(player, Jingle.JOB_CHANGE);
                tab.openScreen(player);
            });
        }
    }

    public static void change(ServerPlayer player, String from, String to) {
        if (isPicked(player, from) && ARCHETYPES.contains(to) && !isPicked(player, to)) {
            Optional<Category> classes = SkillsAPI.getCategory(ArchetypeTabs.CLASSES);
            Optional<Category> oldTab = tab(from);
            Optional<Category> newTab = tab(to);
            Optional<Skill> oldNode = classes.flatMap(c -> c.getSkill(from));
            Optional<Skill> newNode = classes.flatMap(c -> c.getSkill(to));
            if (!oldTab.isEmpty() && !newTab.isEmpty() && !oldNode.isEmpty() && !newNode.isEmpty()) {
                int level = keptLevel(oldTab.get().getExperience().map(e -> e.getLevel(player)).orElse(0));
                oldNode.get().lock(player);
                newTab.get().erase(player);
                newNode.get().unlock(player);
                newTab.get().getExperience().ifPresent(e -> e.setLevel(player, level));
                AbilityActions.dropUngranted(player);
                player.displayClientMessage(Component.translatable("message.mdvlcraft.respec.changed", title(from), title(to), level), false);
                newTab.get().openScreen(player);
            }
        }
    }

    public static boolean isPicked(ServerPlayer player, String archetype) {
        return ARCHETYPES.contains(archetype)
            && SkillsAPI.getCategory(ArchetypeTabs.CLASSES).flatMap(c -> c.getSkill(archetype)).map(s -> s.getState(player) == State.UNLOCKED).orElse(false);
    }

    public static String classOf(String archetype) {
        String type = CLASS.get(archetype);
        if (type == null) {
            throw new IllegalArgumentException("Not an archetype: " + archetype);
        }

        return type;
    }

    static Optional<Category> tab(String archetype) {
        return SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", archetype));
    }

    private static String title(String archetype) {
        return Character.toUpperCase(archetype.charAt(0)) + archetype.substring(1);
    }
}
