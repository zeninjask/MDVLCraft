package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.mixin.EmeraldsForVillagerTypeItemAccessor;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        return new Tuned(listing instanceof EmeraldsForVillagerTypeItemAccessor typed ? new BiomeItemForEmerald(typed) : listing);
    }

    /**
     * Vanilla's trade for the item of the villager's biome (a fisherman's boat), rebuilt: Distant Horizons bundles a
     * Fabric API mixin into it that expects a local variable Forge's version doesn't have, so calling it throws and stops
     * the server.
     */
    private record BiomeItemForEmerald(Map<VillagerType, Item> trades, int cost, int maxUses, int villagerXp) implements ItemListing {
        BiomeItemForEmerald(EmeraldsForVillagerTypeItemAccessor vanilla) {
            this(vanilla.mdvlcraft$trades(), vanilla.mdvlcraft$cost(), vanilla.mdvlcraft$maxUses(), vanilla.mdvlcraft$villagerXp());
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            Item item = trader instanceof VillagerDataHolder holder ? this.trades.get(holder.getVillagerData().getType()) : null;
            return item == null ? null : new MerchantOffer(new ItemStack(item, this.cost), new ItemStack(Items.EMERALD), this.maxUses, this.villagerXp, 0.05F);
        }
    }

    /** A trade with the pack's experience and restock; command/ObtainabilityReport looks inside it. */
    public record Tuned(ItemListing listing) implements ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            MerchantOffer offer = this.listing.getOffer(trader, random);
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
        }
    }
}
