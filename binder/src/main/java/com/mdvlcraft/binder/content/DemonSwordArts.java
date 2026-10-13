package com.mdvlcraft.binder.content;

import com.tiago.antimagic.item.DemonDestroyerSword;
import com.tiago.antimagic.item.DemonDwellerSword;
import com.tiago.antimagic.item.DemonSlayerSword;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A demon sword's own art (Demon Slayer: Anti-Magic Shroud, Demon Dweller: Anti-Magic Slash, Demon Destroyer: Causality
 * Break), cast when the client reports both mouse buttons held. The Anti-Magic Slash then has a 12 second cooldown.
 */
public final class DemonSwordArts {
    private static final int SLASH_COOLDOWN_TICKS = 240;
    private static boolean casting;

    private DemonSwordArts() {
    }

    public static boolean isDemonSword(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DemonSlayerSword || item instanceof DemonDwellerSword || item instanceof DemonDestroyerSword;
    }

    /** True while cast runs the sword's use, which DemonSwordUseMixin otherwise blocks. */
    public static boolean casting() {
        return casting;
    }

    public static void cast(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();
        if (!isDemonSword(stack) || player.getCooldowns().isOnCooldown(item)) {
            return;
        }

        casting = true;
        try {
            item.use(player.level(), player, InteractionHand.MAIN_HAND);
        } finally {
            casting = false;
        }

        if (item instanceof DemonDwellerSword) {
            player.getCooldowns().addCooldown(item, SLASH_COOLDOWN_TICKS);
        }
    }
}
