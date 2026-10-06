package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;
import java.util.Optional;

/**
 * Ties the Classes tab to the archetype tabs. The four class skills are unlocked for every player on login; unlocking an
 * archetype skill (whose ID equals the archetype tab's ID) unlocks that tab, and locking it erases the tab.
 */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class ArchetypeTabs {
    public static final ResourceLocation CLASSES = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "classes");
    private static final List<String> CLASS_SKILLS = List.of("warrior", "mage", "rogue", "ranger");

    private ArchetypeTabs() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> archetypeTab(categoryId, skillId).ifPresent(tab -> tab.unlock(player)));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> archetypeTab(categoryId, skillId).ifPresent(tab -> tab.erase(player)));
    }

    private static Optional<Category> archetypeTab(ResourceLocation categoryId, String skillId) {
        if (!categoryId.equals(CLASSES) || CLASS_SKILLS.contains(skillId)) {
            return Optional.empty();
        }
        return Optional.of(SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, skillId))
                .orElseThrow(() -> new IllegalStateException("Archetype " + skillId + " has no skill tab")));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Category classes = SkillsAPI.getCategory(CLASSES).orElseThrow(() -> new IllegalStateException("Skill tab " + CLASSES + " is missing"));
        for (String classSkill : CLASS_SKILLS) {
            Skill skill = classes.getSkill(classSkill).orElseThrow(() -> new IllegalStateException("Class skill " + classSkill + " is missing"));
            if (skill.getState(player) != Skill.State.UNLOCKED) {
                skill.unlock(player);
            }
        }
    }
}
