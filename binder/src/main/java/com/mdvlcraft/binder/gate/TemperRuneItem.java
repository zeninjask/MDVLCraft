package com.mdvlcraft.binder.gate;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A tempering rune of one rank, bought at the Gate Forge and combined with gear in an anvil (GateTempering). */
public class TemperRuneItem extends Item {
    private final GateRank rank;

    public TemperRuneItem(GateRank rank) {
        super(new Item.Properties().stacksTo(16).rarity(GateItems.rarity(rank)));
        this.rank = rank;
    }

    public GateRank rank() {
        return this.rank;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mdvlcraft.temper_rune.desc", GateTempering.percent(this.rank)).withStyle(ChatFormatting.GRAY));
    }
}
