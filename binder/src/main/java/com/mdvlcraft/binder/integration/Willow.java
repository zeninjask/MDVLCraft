package com.mdvlcraft.binder.integration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Willow, Cursed Fate's jujutsu trader, only comes when called. Willow's Call (a gold ingot ringed by curse residue) works
 * for 10 minutes after it is crafted, then fades; using it brings Willow beside you for 10 minutes. She never spawns on her
 * own. Talking to her opens a vanilla trading screen (it scrolls) with all her trades that are kept: every weapon (and the
 * JujutsuCraft weapons, orca/OrcaWeapons: 10 tokens, Playful Cloud 25), curse
 * residue into jujutsu tokens, and the inventory cursed spirit. Her Epic Fight patch (WillowPatch) animates her model.
 */
public final class Willow {
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("cursedfate", "willow_the_jujutsu_trader");
    private static final String CALLED = "mdvlcraft_willow";
    private static final String LEAVES = "mdvlcraft_willow_leaves";
    private static final String CRAFTED = "mdvlcraft_crafted";
    private static final int WINDOW = 12000;   // 10 minutes
    private static final int STAY = 12000;
    private static final String[] WEAPONS = {"inverted_spearof_heaven", "dragon_bone", "naginata", "nanamis_cleaver", "split_soul_katana",
        "crow_sorcerer_axe", "jet_black_blade", "slaughter_demon", "hiten", "kamutoke"};
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> CALL = ITEMS.register("willow_call", CallItem::new);

    private Willow() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        ITEMS.register(modBus);
        forgeBus.addListener(Willow::onJoin);
        forgeBus.addListener(Willow::onInteract);
        forgeBus.addListener(Willow::onTick);
        forgeBus.addListener(Willow::onCrafted);
    }

    public static boolean isWillow(Entity entity) {
        return TYPE.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    private static ItemStack stack(String id, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("cursedfate", id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static MerchantOffers offers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(stack("curse_residue", 30), stack("jujutsu_token", 1), Integer.MAX_VALUE, 0, 0.0F));
        offers.add(new MerchantOffer(stack("jujutsu_token", 5), stack("inventory_cursed_spirit_item", 1), Integer.MAX_VALUE, 0, 0.0F));
        for (String weapon : WEAPONS) {
            offers.add(new MerchantOffer(stack("jujutsu_token", 10), stack(weapon, 1), Integer.MAX_VALUE, 0, 0.0F));
        }
        // JujutsuCraft's weapons (orca/OrcaWeapons), at their own prices
        com.mdvlcraft.binder.orca.OrcaWeapons.PRICES.forEach((weapon, price) ->
            offers.add(new MerchantOffer(stack("jujutsu_token", price), new ItemStack(weapon.get()), Integer.MAX_VALUE, 0, 0.0F)));
        offers.removeIf(offer -> offer.getBaseCostA().isEmpty() || offer.getResult().isEmpty());
        return offers;
    }

    // ---- Willow herself ----

    /** She only comes when called: any other Willow (natural spawn, spawn egg, Cursed Fate's own) is turned away. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isWillow(event.getEntity()) && !event.getEntity().getPersistentData().getBoolean(CALLED)) {
            event.setCanceled(true);
        }
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isWillow(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            new Trader(player).openTradingScreen(player, event.getTarget().getDisplayName(), 0);
        }
    }

    /** She leaves when her 10 minutes are up. */
    private static void onTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 20 == 0 && entity.level() instanceof ServerLevel level && isWillow(entity)
            && level.getGameTime() >= entity.getPersistentData().getLong(LEAVES)) {
            level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY(1.0), entity.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0F, 0.8F);
            entity.discard();
        }
    }

    // ---- Willow's Call ----

    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(CALL.get()) && !event.getEntity().level().isClientSide) {
            event.getCrafting().getOrCreateTag().putLong(CRAFTED, event.getEntity().level().getGameTime());
        }
    }

    private static final class CallItem extends Item {
        CallItem() {
            super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        }

        /** Ticks left to use it (a call that was never stamped, e.g. from creative, starts its 10 minutes now). */
        private static long left(ItemStack stack, Level level) {
            if (!stack.getOrCreateTag().contains(CRAFTED)) {
                stack.getOrCreateTag().putLong(CRAFTED, level.getGameTime());
            }
            return stack.getOrCreateTag().getLong(CRAFTED) + WINDOW - level.getGameTime();
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
            if (!level.isClientSide && holder.tickCount % 20 == 0 && left(stack, level) <= 0) {
                stack.setCount(0);
                if (holder instanceof Player player) {
                    player.displayClientMessage(Component.translatable("item.mdvlcraft.willow_call.faded").withStyle(ChatFormatting.GRAY), true);
                }
            }
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(level instanceof ServerLevel server)) {
                return InteractionResultHolder.success(stack);
            }
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(TYPE);
            Entity willow = type == null || left(stack, level) <= 0 ? null : type.create(server);
            if (willow == null) {
                return InteractionResultHolder.fail(stack);
            }
            Vec3 at = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.0));
            willow.moveTo(at.x, player.getY(), at.z, player.getYRot() + 180.0F, 0.0F);
            willow.getPersistentData().putBoolean(CALLED, true);
            willow.getPersistentData().putLong(LEAVES, level.getGameTime() + STAY);
            if (willow instanceof Mob mob) {
                mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                mob.setPersistenceRequired();
            }
            server.addFreshEntity(willow);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1.0, at.z, 40, 0.4, 0.8, 0.4, 0.03);
            level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8F, 1.2F);
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.willow_call.desc").withStyle(ChatFormatting.GRAY));
            if (level != null && stack.hasTag() && stack.getTag().contains(CRAFTED)) {
                long seconds = Math.max(0, stack.getTag().getLong(CRAFTED) + WINDOW - level.getGameTime()) / 20;
                tooltip.add(Component.translatable("item.mdvlcraft.willow_call.left", seconds / 60, String.format("%02d", seconds % 60))
                    .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }

    /** One player's session with Willow (server side; the client gets a ClientSideMerchant from the menu packet). */
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
            return SoundEvents.WANDERING_TRADER_YES;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }
    }
}
