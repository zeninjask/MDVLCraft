package com.mdvlcraft.binder.content;

import java.util.List;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Villager trades give 35% less experience and restock after 25% fewer uses. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TradeTuning {
    private static final double XP_SCALE = 0.65;
    private static final double USES_SCALE = 0.75;

    private TradeTuning() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(TradeTuning::wrapAll);
    }

    private static void wrapAll(List<ItemListing> listings) {
        listings.replaceAll(TradeTuning::wrap);
    }

    private static ItemListing wrap(ItemListing listing) {
        return (trader, random) -> {
            MerchantOffer offer = listing.getOffer(trader, random);
            return offer == null ? null : new MerchantOffer(
                offer.getBaseCostA(),
                offer.getCostB(),
                offer.getResult(),
                offer.getUses(),
                Math.max(1, (int)Math.round(offer.getMaxUses() * USES_SCALE)),
                (int)Math.round(offer.getXp() * XP_SCALE),
                offer.getPriceMultiplier(),
                offer.getDemand()
            );
        };
    }
}
