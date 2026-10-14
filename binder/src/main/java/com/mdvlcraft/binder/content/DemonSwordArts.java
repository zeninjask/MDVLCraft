package com.mdvlcraft.binder.content;

import com.tiago.antimagic.item.DemonDestroyerSword;
import com.tiago.antimagic.item.DemonDwellerSword;
import com.tiago.antimagic.item.DemonSlayerSword;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * A demon sword's own art. The Demon Slayer's Anti-Magic Shroud and the Demon Destroyer's Causality Break are cast when
 * the client reports both mouse buttons held; the Demon Dweller's Anti-Magic Slash is its innate skill
 * (DemonDwellerSlashSkill), which fires the slash here once its swing reaches the hit.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DemonSwordArts {
    /** Players with a slash on its way, and the ticks left before it leaves the blade. */
    private static final Map<ServerPlayer, Integer> PENDING_SLASHES = new WeakHashMap<>();
    private static boolean casting;

    private DemonSwordArts() {
    }

    public static boolean isDemonSword(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DemonSlayerSword || item instanceof DemonDwellerSword || item instanceof DemonDestroyerSword;
    }

    /** Swords whose art is cast with both mouse buttons. */
    public static boolean hasBothButtonArt(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DemonSlayerSword || item instanceof DemonDestroyerSword;
    }

    /** True while cast runs the sword's use, which DemonSwordUseMixin otherwise blocks. */
    public static boolean casting() {
        return casting;
    }

    public static void cast(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();
        if (!hasBothButtonArt(stack) || player.getCooldowns().isOnCooldown(item)) {
            return;
        }

        use(player, item);
    }

    /** Fires the Demon Dweller's slash after the given ticks, if it is still in hand. */
    public static void slashAfter(ServerPlayer player, int ticks) {
        PENDING_SLASHES.put(player, ticks);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        Integer ticks = PENDING_SLASHES.get(player);
        if (ticks == null) {
            return;
        }

        if (ticks > 1) {
            PENDING_SLASHES.put(player, ticks - 1);
            return;
        }

        PENDING_SLASHES.remove(player);
        Item item = player.getMainHandItem().getItem();
        if (item instanceof DemonDwellerSword && player.isAlive()) {
            use(player, item);
        }
    }

    private static void use(ServerPlayer player, Item item) {
        casting = true;
        try {
            item.use(player.level(), player, InteractionHand.MAIN_HAND);
        } finally {
            casting = false;
        }
    }
}
