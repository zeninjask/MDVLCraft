package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.mixin.puffish.SkillsClientModAccessor;
import com.mdvlcraft.binder.skills.ArchetypeTabs;
import com.mdvlcraft.binder.skills.Respec;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.puffish.skillsmod.api.Skill.State;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.data.ClientCategoryData;

/** The local player's vocation as the skill screen knows it: their vocation tab and picked archetypes. */
final class ClientVocation {
    private ClientVocation() {
    }

    /** The vocation tab (mdvlcraft:voc_<a>_<b>), once two archetypes are picked. */
    static Optional<ClientCategoryData> tab() {
        return ((SkillsClientModAccessor)SkillsClientMod.getInstance())
            .mdvlcraft$screenData()
            .streamCategories()
            .filter(c -> c.getConfig().id().getNamespace().equals("mdvlcraft") && c.getConfig().id().getPath().startsWith("voc_"))
            .findFirst();
    }

    /** The picked archetypes, in Respec.ARCHETYPES order. */
    static List<String> archetypes() {
        Optional<ClientCategoryData> tab = tab();
        if (tab.isPresent()) {
            return Arrays.asList(tab.get().getConfig().id().getPath().substring("voc_".length()).split("_"));
        }

        return ((SkillsClientModAccessor)SkillsClientMod.getInstance())
            .mdvlcraft$screenData()
            .streamCategories()
            .filter(c -> c.getConfig().id().equals(ArchetypeTabs.ARCHETYPES))
            .findFirst()
            .map(page -> Respec.ARCHETYPES.stream().filter(a -> {
                ClientSkillConfig node = page.getConfig().skills().get(a);
                return node != null && page.getSkillState(node) == State.UNLOCKED;
            }).toList())
            .orElse(List.of());
    }

    /** The vocation's name (the tab's title), the one archetype's name, or "n/a". */
    static String name() {
        Optional<ClientCategoryData> tab = tab();
        if (tab.isPresent()) {
            return tab.get().getConfig().title().getString();
        }

        List<String> picked = archetypes();
        return picked.isEmpty() ? "n/a" : Respec.title(picked.get(0));
    }
}
