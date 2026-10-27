package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SettingsSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Keep Slot Open setting (off by default, client/SettingsScreen): the first hotbar slot stays empty, so selecting it
 * always gives an empty main hand. It uses the Mage Hunter slot's guards (content/MageHunterSlot.isSwordSlot): nothing
 * goes in by clicking, number keys or pickups, and F can't swap the off hand into it. An item already there when the
 * setting is turned on, or put there by anything else, goes into the bag, or on the ground if the bag is full.
 * Players with the Samurai or Mage Hunter slot keep theirs; the setting does nothing for them. Kept in the player's
 * persisted data, so it holds through deaths and relogs.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class KeepSlotOpen {
    private static final String KEY = "mdvlcraft_keep_slot_open";
    /** The local player's setting, as the server last sent it. */
    private static boolean client;

    private KeepSlotOpen() {
    }

    public static boolean isOn(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return data(serverPlayer).getBoolean(KEY);
        }
        return player != null && player.level().isClientSide && client;
    }

    /** Whether the player's first hotbar slot is kept empty: the setting is on and they have no Samurai or Mage Hunter slot. */
    public static boolean applies(Player player) {
        return !player.isCreative() && isOn(player) && !MageHunterSlot.isMageHunter(player);
    }

    public static void set(ServerPlayer player, boolean on) {
        data(player).putBoolean(KEY, on);
        if (applies(player)) {
            clear(player);
        }
        sync(player);
    }

    public static void setClient(boolean on) {
        client = on;
    }

    public static void sync(ServerPlayer player) {
        BinderNetwork.sendTo(player, new SettingsSyncPacket(isOn(player)));
    }

    /** Moves whatever is in the slot into the bag (which skips the slot, mixin/InventoryMixin), or drops it. */
    private static void clear(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack held = inventory.getItem(MageHunterSlot.SLOT);
        if (held.isEmpty()) {
            return;
        }

        inventory.setItem(MageHunterSlot.SLOT, ItemStack.EMPTY);
        if (!inventory.add(held)) {
            player.drop(held, false);
        }
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        sync((ServerPlayer)event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && applies(player)) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player player && player.getInventory().selected == MageHunterSlot.SLOT && applies(player)) {
            event.setCanceled(true);
        }
    }
}
