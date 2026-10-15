package com.mdvlcraft.binder.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill.State;

/**
 * The Archetypes page (mdvlcraft:archetypes): the three class nodes are unlocked for everyone on login, and picking a
 * second archetype opens that pair's vocation tab. Dropping an archetype (a respec) erases the old vocation tab.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ArchetypeTabs {
    public static final ResourceLocation ARCHETYPES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "archetypes");
    private static final List<String> CLASS_SKILLS = List.of("warrior", "mage", "rogue");

    private ArchetypeTabs() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> {
            if (isArchetypeNode(categoryId, skillId)) {
                Respec.vocation(player).ifPresent(tab -> {
                    if (!tab.isUnlocked(player)) {
                        tab.unlock(player);
                    }
                });
            }
        });
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> {
            if (isArchetypeNode(categoryId, skillId)) {
                List<String> rest = Respec.picked(player);
                if (rest.size() == 1) {
                    Respec.tab(rest.get(0), skillId).erase(player);   // already erased by a respec; harmless
                }
            }
        });
    }

    /** A node of the Archetypes page that picks an archetype (not one of the class nodes). */
    public static boolean isArchetypeNode(ResourceLocation categoryId, String skillId) {
        return categoryId.equals(ARCHETYPES) && !CLASS_SKILLS.contains(skillId);
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        Category page = SkillsAPI.getCategory(ARCHETYPES).orElseThrow(() -> new IllegalStateException("Skill tab " + ARCHETYPES + " is missing"));
        for (String classSkill : CLASS_SKILLS) {
            Skill skill = page.getSkill(classSkill).orElseThrow(() -> new IllegalStateException("Class skill " + classSkill + " is missing"));
            if (skill.getState(player) != State.UNLOCKED) {
                skill.unlock(player);
            }
        }

        Optional<Category> vocation = Respec.vocation(player);
        vocation.filter(tab -> !tab.isUnlocked(player)).ifPresent(tab -> tab.unlock(player));
    }
}
