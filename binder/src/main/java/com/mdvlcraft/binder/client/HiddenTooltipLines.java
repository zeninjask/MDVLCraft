package com.mdvlcraft.binder.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/** Tooltips never show an item's registry id or its "NBT: n tag(s)" line, advanced tooltips (F3+H) or not. */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class HiddenTooltipLines {
    private HiddenTooltipLines() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
        event.getToolTip().removeIf(line -> line.getString().equals(id) || isNbtLine(line));
    }

    private static boolean isNbtLine(Component line) {
        return line.getContents() instanceof TranslatableContents translatable && "item.nbt_tags".equals(translatable.getKey());
    }
}
