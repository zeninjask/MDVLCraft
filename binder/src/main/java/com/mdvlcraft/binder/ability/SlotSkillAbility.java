package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.epicskill.EpicSkillEquip;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.Arrays;
import java.util.Optional;

/**
 * An Epic Fight skill that lives in an add-on's own skill slot, cast from the ability wheel instead of the add-on's key
 * (its key is unbound): Epic Fight Nightfall's Soul Hunt. Casting
 * puts the skill in its slot if it is not there yet, then runs it exactly as the add-on's key would, with the skill's
 * own conditions (on the ground, a weapon or sword in hand) and cooldown. Mana is paid only when it runs.
 */
public enum SlotSkillAbility implements Ability {
    SOUL_HUNT("soul_hunt", "efn:execution", 20, "efn:textures/gui/skills/efn_arts/execution.png");

    private final ResourceLocation id;
    private final String skillId;
    private final int manaCost;
    private final ResourceLocation icon;

    SlotSkillAbility(String name, String skillId, int manaCost, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, name);
        this.skillId = skillId;
        this.manaCost = manaCost;
        this.icon = ResourceLocation.parse(icon);
    }

    public static Optional<SlotSkillAbility> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(a -> a.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft." + id.getPath());
    }

    @Override
    public ResourceLocation icon() {
        return icon;
    }

    @Override
    public int manaCost(int level) {
        return manaCost;
    }

    void cast(ServerPlayer player) {
        Skill skill = SkillManager.getSkill(skillId);
        if (skill == null) {
            throw new IllegalStateException("Epic Fight skill " + skillId + " is not registered");
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        SkillSlot slot = SkillSlot.ENUM_MANAGER.universalValues().stream()
                .filter(s -> s.category() == skill.getCategory())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No skill slot for " + skillId));
        SkillContainer container = patch.getSkill(slot);
        if (container.getSkill() != skill) {
            EpicSkillEquip.apply(patch, container, skill);
        }
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (!player.isCreative() && magic.getMana() < manaCost) {
            actionBar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", displayName()));
            return;
        }
        if (!skill.canExecute(container)) {
            actionBar(player, Component.translatable("ability.mdvlcraft.cannot_cast", displayName()));
            return;
        }
        if (!player.isCreative()) {
            magic.setMana(magic.getMana() - manaCost);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        }
        skill.executeOnServer(container, new FriendlyByteBuf(Unpooled.buffer()));
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }
}
