package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Removes Iron's Spells and Cursed Fate mobs, trades, creative-tab entries and drops from normal play. */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class ContentEvents {
    /** Spawn reasons still allowed: summons created by granted spells/techniques, and commands. */
    private static final Set<MobSpawnType> ALLOWED_SPAWNS = EnumSet.of(MobSpawnType.MOB_SUMMONED, MobSpawnType.COMMAND);

    private ContentEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (DisabledContent.isDisabled(event.getEntity().getType()) && !ALLOWED_SPAWNS.contains(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(ContentEvents::filterListings);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWandererTrades(WandererTradesEvent event) {
        filterListings(event.getGenericTrades());
        filterListings(event.getRareTrades());
    }

    private static void filterListings(List<VillagerTrades.ItemListing> listings) {
        listings.replaceAll(listing -> (trader, random) -> {
            MerchantOffer offer = listing.getOffer(trader, random);
            return offer == null || usesDisabledItem(offer) ? null : offer;
        });
    }

    private static boolean usesDisabledItem(MerchantOffer offer) {
        return DisabledContent.isDisabled(offer.getResult())
                || DisabledContent.isDisabled(offer.getBaseCostA())
                || DisabledContent.isDisabled(offer.getCostB());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> disabled = new ArrayList<>();
        for (Map.Entry<ItemStack, ?> entry : event.getEntries()) {
            if (DisabledContent.isDisabled(entry.getKey())) {
                disabled.add(entry.getKey());
            }
        }
        disabled.forEach(event.getEntries()::remove);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> DisabledContent.isDisabled(drop.getItem()));
    }
}
