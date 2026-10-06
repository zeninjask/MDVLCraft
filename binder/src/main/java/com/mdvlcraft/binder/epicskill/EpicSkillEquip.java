package com.mdvlcraft.binder.epicskill;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import javax.annotation.Nullable;

/**
 * Equipping Epic Fight skills from the abilities screen. Unlike Epic Fight's own packet (which the binder ignores, see
 * CPChangeSkillMixin), the server checks the player has learned the skill and that it fits the slot.
 */
public final class EpicSkillEquip {
    private EpicSkillEquip() {
    }

    /** A client asked to put a skill in a slot (or empty it, with null). */
    public static void request(ServerPlayer player, SkillSlot slot, @Nullable Skill skill) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null || !slot.category().learnable()) {
            return;
        }
        if (skill != null && (skill.getCategory() != slot.category() || !patch.getSkillCapability().hasLearned(skill))) {
            return;
        }
        SkillContainer container = patch.getSkill(slot);
        if (container.getSkill() == skill) {
            return;
        }
        if (container.onReplaceCooldown()) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    "screen.mdvlcraft.skills.on_cooldown", container.getReplaceCooldown() / 20).withStyle(ChatFormatting.RED)));
            return;
        }
        if (skill != null) {
            SkillContainer other = patch.getSkillCapability().getSkillContainer(skill);
            if (other != null) {
                apply(patch, other, null);
            }
        }
        apply(patch, container, skill);
        int cooldown = EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.level());
        container.setReplaceCooldown(cooldown);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(slot, cooldown, player.getId()), player);
    }

    /** Puts the skill in the container on the server, the player's client and the clients tracking them. */
    public static void apply(ServerPlayerPatch patch, SkillContainer container, @Nullable Skill skill) {
        ServerPlayer player = patch.getOriginal();
        container.setSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPChangeSkill(container.getSlot(), player.getId(), skill), player);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPSetRemotePlayerSkill(player.getId(), container.getSlot(), skill), player);
    }
}
