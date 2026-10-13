package com.mdvlcraft.binder.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Every player gets the MDVLCraft guide once, on first join. The pages are translation keys (guide.mdvlcraft.page.N in the
 * lang file), so the text is edited there, and key names inside it follow the player's own key bindings.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GuideBook {
    private static final String GIVEN = "mdvlcraft_guide_given";
    private static final int PAGES = 9;
    /** %1$s menu, %2$s ability wheel, %3$s cast, %4$s give up: each shows the player's current binding. */
    private static final Object[] KEYS = {
        Component.keybind("key.mdvlcraft.menu"), Component.keybind("key.mdvlcraft.ability_wheel"),
        Component.keybind("key.mdvlcraft.cast_ability"), Component.keybind("key.mdvlcraft.give_up")
    };

    private GuideBook() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(GIVEN)) {
            data.putBoolean(GIVEN, true);
            player.getInventory().add(book());
        }
    }

    private static ItemStack book() {
        ListTag pages = new ListTag();
        for (int page = 1; page <= PAGES; page++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("guide.mdvlcraft.page." + page, KEYS))));
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "MDVLCraft Guide");
        tag.putString("author", "MDVLCraft");
        tag.put("pages", pages);
        return book;
    }
}
