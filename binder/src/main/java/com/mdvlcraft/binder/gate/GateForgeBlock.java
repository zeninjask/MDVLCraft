package com.mdvlcraft.binder.gate;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Gate Forge (plan 6.1), where essence stones and mana crystals are spent. A minimal version: using it opens the vanilla
 * trading screen with fixed, unlimited trades, so it needs no menu or screen of its own:
 *   Return Stone        2 E essence stones + 1 E mana crystal
 *   <rank> Temper Rune  6 essence stones + 2 mana crystals of that rank (the rune tempers gear in an anvil, GateTempering)
 * Using it with an A or S key in hand rerolls the key's modifiers (GateModifiers.reroll). The tower's seal trades come from
 * TowerItems.forgeOffers.
 */
public class GateForgeBlock extends Block {
    public GateForgeBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL));
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (GateModifiers.reroll(player, hand)) {   // with an A/S key in hand: reroll its modifiers
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            new Trader(player).openTradingScreen(player, Component.translatable("block.mdvlcraft.gate_forge"), 0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static MerchantOffers offers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(new ItemStack(GateItems.ESSENCE_STONES.get(GateRank.E).get(), 2), new ItemStack(GateItems.MANA_CRYSTALS.get(GateRank.E).get()),
            new ItemStack(GateRewards.RETURN_STONE.get())));
        for (GateRank rank : GateRank.values()) {
            offers.add(offer(new ItemStack(GateItems.ESSENCE_STONES.get(rank).get(), 6), new ItemStack(GateItems.MANA_CRYSTALS.get(rank).get(), 2),
                new ItemStack(GateRewards.TEMPER_RUNES.get(rank).get())));
        }
        offers.addAll(com.mdvlcraft.binder.gate.tower.TowerItems.forgeOffers());   // Tower Seal trades
        return offers;
    }

    private static MerchantOffer offer(ItemStack costA, ItemStack costB, ItemStack result) {
        return new MerchantOffer(costA, costB, result, Integer.MAX_VALUE, 0, 0.0F);
    }

    /** One player's session at the forge (server side; the client gets a ClientSideMerchant from the menu packet). */
    private static final class Trader implements Merchant {
        private Player player;
        private MerchantOffers offers = offers();

        Trader(Player player) {
            this.player = player;
        }

        @Override
        public void setTradingPlayer(Player player) {
            this.player = player;
        }

        @Override
        public Player getTradingPlayer() {
            return this.player;
        }

        @Override
        public MerchantOffers getOffers() {
            return this.offers;
        }

        @Override
        public void overrideOffers(MerchantOffers offers) {
            this.offers = offers;
        }

        @Override
        public void notifyTrade(MerchantOffer offer) {
            offer.increaseUses();
        }

        @Override
        public void notifyTradeUpdated(ItemStack stack) {
        }

        @Override
        public int getVillagerXp() {
            return 0;
        }

        @Override
        public void overrideXp(int xp) {
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public SoundEvent getNotifyTradeSound() {
            return SoundEvents.SMITHING_TABLE_USE;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }
    }
}
