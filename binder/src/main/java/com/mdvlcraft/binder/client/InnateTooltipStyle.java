package com.mdvlcraft.binder.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Every weapon's innate skill tooltip (the view Epic Fight shows while its tooltip key is held) in Epic Fight's own style,
 * whatever mod wrote it (Weapons of Miracles, Nightfall, Invincible combos ...): the skill's name in white with its cost
 * [n] in aqua, then the text in dark grey, headings (bold, underlined or ending in ':') in underlined grey. Lines already
 * in Epic Fight's style (its per-hit stat lines: dark grey with coloured values) are kept as they are.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class InnateTooltipStyle {
    private static final TextColor DARK_GRAY = TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY);
    private static final TextColor GRAY = TextColor.fromLegacyFormat(ChatFormatting.GRAY);

    private InnateTooltipStyle() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || !InputManager.isActionPhysicallyActive(EpicFightInputAction.WEAPON_INNATE_SKILL_TOOLTIP)) {
            return;
        }
        LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LocalPlayerPatch.class);
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapabilityOr(event.getItemStack(), null);
        Skill skill = playerpatch == null || capability == null ? null : capability.getInnateSkill(playerpatch, event.getItemStack());
        List<Component> lines = event.getToolTip();
        if (skill == null || lines.isEmpty()) {
            return;
        }
        lines.set(0, Component.translatable(skill.getTranslationKey()).withStyle(ChatFormatting.WHITE)
            .append(Component.literal(String.format("[%.0f]", skill.getConsumption())).withStyle(ChatFormatting.AQUA)));
        for (int i = 1; i < lines.size(); i++) {
            lines.set(i, restyle(lines.get(i)));
        }
    }

    private static Component restyle(Component line) {
        Style style = line.getStyle();
        if (DARK_GRAY.equals(style.getColor()) || GRAY.equals(style.getColor()) && style.isUnderlined()) {
            return line;
        }
        String text = ChatFormatting.stripFormatting(line.getString());
        boolean heading = style.isBold() || style.isUnderlined() || text.strip().endsWith(":");
        return heading ? Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE)
            : Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }
}
