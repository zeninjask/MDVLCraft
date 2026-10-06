package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.SlotSkillAbility;
import com.mdvlcraft.binder.ability.SpellAbility;
import com.mdvlcraft.binder.ability.StanceAbility;
import com.mdvlcraft.binder.ability.StanceElement;
import com.mdvlcraft.binder.ability.Technique;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** How an ability is drawn: its 16x16 icon, cooldown shade and tooltip. */
final class AbilityDisplay {
    private AbilityDisplay() {
    }

    static void drawIcon(GuiGraphics graphics, Ability ability, int x, int y) {
        if (ability instanceof StanceAbility) {
            // The current element; dimmed while the stance is off.
            int self = Minecraft.getInstance().player.getId();
            graphics.blit(StanceAbility.icon(ClientStances.element(self)), x, y, 0, 0, 16, 16, 16, 16);
            if (ClientStances.active(self).isEmpty()) {
                graphics.fill(x, y, x + 16, y + 16, 0x90000000);
            }
            return;
        }
        graphics.blit(ability.icon(), x, y, 0, 0, 16, 16, 16, 16);
        float cooldown = ability instanceof SpellAbility spell ? ClientMagicData.getCooldownPercent(spell.spell()) : 0;
        if (cooldown > 0) {
            int height = Math.round(16 * cooldown);
            graphics.fill(x, y + 16 - height, x + 16, y + 16, 0xA0000000);
        }
    }

    static List<Component> tooltip(Ability ability, int level) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> lines = new ArrayList<>();
        if (ability instanceof SpellAbility spell) {
            lines.addAll(TooltipsUtils.formatActiveSpellTooltip(ItemStack.EMPTY, new SpellData(spell.spell(), level), CastSource.SPELLBOOK, minecraft.player));
            lines.remove(0); // Iron's leads with a blank line meant for item tooltips.
        } else if (ability instanceof Technique technique) {
            lines.add(technique.displayName().copy().withStyle(ChatFormatting.DARK_RED));
            lines.add(Component.translatable("ability.mdvlcraft." + technique.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", technique.manaCost(level)).withStyle(ChatFormatting.BLUE));
            lines.add(Component.translatable("ability.mdvlcraft.no_cooldown").withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof SlotSkillAbility slotSkill) {
            lines.add(slotSkill.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft." + slotSkill.id().getPath() + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("ui.irons_spellbooks.mana_cost", slotSkill.manaCost(level)).withStyle(ChatFormatting.BLUE));
        } else if (ability instanceof StanceAbility stance) {
            int self = minecraft.player.getId();
            StanceElement element = ClientStances.element(self);
            lines.add(stance.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ability.mdvlcraft.stance.desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(ClientStances.active(self).isPresent() ? "stance.mdvlcraft.tooltip_on" : "stance.mdvlcraft.tooltip_off",
                    element.displayName(), element.effect()));
        }
        return lines;
    }
}
