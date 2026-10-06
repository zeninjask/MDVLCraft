package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.SpellAbility;
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
        }
        return lines;
    }
}
