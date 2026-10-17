package com.mdvlcraft.binder.mixin.jer;

import com.mdvlcraft.binder.compat.jer.JerLoot;
import com.mdvlcraft.binder.content.DisabledContent;
import jeresources.collection.TradeList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** JER's villager pages. */
@Mixin(value = TradeList.class, remap = false)
public abstract class TradeListMixin {
    @Shadow
    public abstract void addITradeList(ItemListing[] listings);

    /**
     * JER stops reading a level's trades at the first one that gives no offer on the client (a treasure map needs the
     * server's level), so the trades after it were missing.
     */
    @Inject(method = "addITradeList", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$eachListing(ItemListing[] listings, CallbackInfo ci) {
        if (listings.length > 1) {
            for (ItemListing listing : listings) {
                try {
                    this.addITradeList(new ItemListing[]{listing});
                } catch (RuntimeException e) {
                    JerLoot.brokenTrade(listing, e);
                }
            }

            ci.cancel();
        }
    }

    /**
     * JER makes its trade lists before it has a villager, so every trade was offered by nobody: a trade that reads its
     * trader (a boat for the villager's biome, Iron's wandering curios) failed. A client-side villager stands in. The trades
     * content/ContentEvents takes off a merchant aren't shown either.
     */
    @Redirect(
        method = "addMerchantRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/VillagerTrades$ItemListing;m_213663_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/trading/MerchantOffer;"
        )
    )
    private MerchantOffer mdvlcraft$tradeable(ItemListing listing, Entity trader, RandomSource random) {
        MerchantOffer offer = listing.getOffer(trader == null ? JerLoot.trader() : trader, random);
        return offer == null
                || DisabledContent.isUntradeable(offer.getResult())
                || DisabledContent.isUntradeable(offer.getBaseCostA())
                || DisabledContent.isUntradeable(offer.getCostB())
            ? null
            : offer;
    }
}
