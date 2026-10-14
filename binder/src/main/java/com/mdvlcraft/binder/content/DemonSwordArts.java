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
 * A demon sword's own art (Demon Slayer: Anti-Magic Shroud, Demon Dweller: Anti-Magic Slash, Demon Destroyer: Causality
 * Break), cast when the client reports both mouse buttons held. The Anti-Magic Slash leaves the blade 0.6 seconds after
 * the click and then has a 12 second cooldown (started at the click).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DemonSwordArts {
    private static final int SLASH_COOLDOWN_TICKS = 240;
    private static final int SLASH_DELAY_TICKS = 12;
    /** Players with a slash on its way, and the ticks left before it leaves the blade. */
    private static final Map<ServerPlayer, Integer> PENDING_SLASHES = new WeakHashMap<>();
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

        if (item instanceof DemonDwellerSword) {
            player.getCooldowns().addCooldown(item, SLASH_COOLDOWN_TICKS);
            PENDING_SLASHES.put(player, SLASH_DELAY_TICKS);
            return;
        }

        use(player, item);
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
