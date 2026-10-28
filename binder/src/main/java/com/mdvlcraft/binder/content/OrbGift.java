package com.mdvlcraft.binder.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * From 2.1.70 every player gets one Orb of Origin the first time they log in (new or returning), so they can try the new
 * races. A flag in the player's persisted data makes it once only; it survives death.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OrbGift {
    private static final String FLAG = "mdvlcraft_orb_gift_2170";
    private static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath("origins", "orb_of_origin");

    private OrbGift() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        Item orb = ForgeRegistries.ITEMS.getValue(ORB);
        if (persisted.getBoolean(FLAG) || orb == null) {
            return;
        }
        persisted.putBoolean(FLAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        ItemStack stack = new ItemStack(orb);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
