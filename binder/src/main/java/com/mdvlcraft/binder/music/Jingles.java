package com.mdvlcraft.binder.music;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.JinglePacket;
import com.mdvlcraft.binder.skills.ArchetypeTabs;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;

/** Skill unlocks play Skill Point Gain; picking or changing an archetype on the Archetypes page plays Job Change. */
public final class Jingles {
    private Jingles() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> {
            if (!categoryId.equals(ArchetypeTabs.ARCHETYPES)) {
                send(player, Jingle.SKILL_POINT_GAIN);
            } else if (ArchetypeTabs.isArchetypeNode(categoryId, skillId)) {
                send(player, Jingle.JOB_CHANGE);
            }
        });
    }

    public static void send(ServerPlayer player, Jingle jingle) {
        BinderNetwork.sendTo(player, new JinglePacket(jingle));
    }
}
