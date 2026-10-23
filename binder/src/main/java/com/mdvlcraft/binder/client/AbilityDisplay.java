package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.DoppelgangerAbility;
import com.mdvlcraft.binder.ability.GauntAbility;
import com.mdvlcraft.binder.ability.PlannedAbility;
import com.mdvlcraft.binder.ability.SlotSkillAbility;
import com.mdvlcraft.binder.ability.SpellAbility;
import com.mdvlcraft.binder.ability.StanceAbility;
import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.ability.SustenanceAbility;
import com.mdvlcraft.binder.ability.Technique;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class AbilityDisplay {
    private AbilityDisplay() {
    }

    static void drawIcon(GuiGraphics graphics, Ability ability, int x, int y) {
        if (ability instanceof StanceAbility) {
            int self = Minecraft.getInstance().player.getId();
            graphics.blit(StanceAbility.icon(ClientStances.element(self)), x, y, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            if (ClientStances.active(self).isEmpty()) {
                graphics.fill(x, y, x + 16, y + 16, -1879048192);
            }
        } else {
            graphics.blit(ability.icon(), x, y, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            float cooldown = ability instanceof SpellAbility spell
                ? ClientMagicData.getCooldownPercent(spell.spell())
                : (ability instanceof Technique technique ? ClientTechniqueCooldowns.percent(technique)
                    : (ability instanceof GauntAbility gaunt ? GauntClient.cooldown(gaunt) : 0.0F));
            if (cooldown > 0.0F) {
                int height = Math.round(16.0F * cooldown);
                graphics.fill(x, y + 16 - height, x + 16, y + 16, -1610612736);
            }
        }
    }

    static List<Component> tooltip(Ability ability, int level) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> lines = new ArrayList<>();
        if (ability instanceof SpellAbility spell) {
            lines.addAll(TooltipsUtils.formatActiveSpellTooltip(ItemStack.EMPTY, new SpellData(spell.spell(), level), CastSource.SPELLBOOK, minecraft.player));
            lines.remove(0);
        } else if (ability instanceof Technique technique) {
            lines.add(technique.displayName().copy().withStyle(ChatFormatting.DARK_RED));
            lines.add(Component.translatable("ability.mdvlcraft." + technique.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", technique.manaCost(level)).withStyle(ChatFormatting.BLUE));
            lines.add(cooldownLine(technique, minecraft.player).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof SlotSkillAbility slotSkill) {
            lines.add(slotSkill.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft." + slotSkill.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", slotSkill.manaCost(level)).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof DoppelgangerAbility doppelganger) {
            lines.add(doppelganger.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft.doppelganger.desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ability.mdvlcraft.doppelganger.upkeep", doppelganger.manaCost(level)).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof GauntAbility gaunt) {
            lines.add(gaunt.displayName().copy().withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.translatable("ability." + gaunt.id().getNamespace() + "." + gaunt.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (ability instanceof SustenanceAbility sustenance) {
            lines.add(sustenance.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft.arcane_sustenance.desc", 1, 4).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", sustenance.manaCost(level)).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof com.mdvlcraft.binder.ability.FormlessSwordAbility formless) {
            lines.add(formless.displayName().copy().withStyle(ChatFormatting.DARK_PURPLE));
            lines.add(Component.translatable("ability.mdvlcraft.formless_sword.desc").withStyle(ChatFormatting.GRAY));
        } else if (ability instanceof PlannedAbility planned) {
            lines.add(planned.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(planned.description().copy().withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ability.mdvlcraft.planned.tooltip").withStyle(ChatFormatting.YELLOW));
        } else if (ability instanceof StanceAbility stance) {
            int self = minecraft.player.getId();
            StanceElement element = ClientStances.element(self);
            lines.add(stance.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft.stance.desc").withStyle(ChatFormatting.GRAY));
            lines.add(
                Component.translatable(
                    ClientStances.active(self).isPresent() ? "stance.mdvlcraft.tooltip_on" : "stance.mdvlcraft.tooltip_off",
                    element.displayName(),
                    element.effect()
                )
            );
        }

        return lines;
    }

    /** The cooldown after the player's cooldown reduction, with the base beside it when they differ. */
    private static MutableComponent cooldownLine(Technique technique, Player player) {
        if (technique.cooldownTicks() == 0) {
            return Component.translatable("ability.mdvlcraft.no_cooldown");
        }

        int effective = ClientTechniqueCooldowns.effectiveTicks(technique, player);
        if (effective == technique.cooldownTicks()) {
            return Component.translatable("ability.mdvlcraft.cooldown", seconds(effective));
        }

        return Component.translatable("ability.mdvlcraft.cooldown_reduced", seconds(effective), seconds(technique.cooldownTicks()));
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }
}
