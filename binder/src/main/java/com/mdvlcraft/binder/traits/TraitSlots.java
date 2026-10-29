package com.mdvlcraft.binder.traits;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.OpenTraitSlotsPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The trait slot machine. Using a Trait Token (no recipe, loot or trade: survival players can't get one yet) rolls a
 * trait and shows it on four reels of iron, diamond and netherite (client/TraitSlotScreen); the player then puts it in a
 * slot or throws it away (network/TraitChoicePacket).
 *
 * The tier is rolled first, 52 Common / 33 Rare / 12 Cursed / 3 Mythic, and the reels are then made to show a payline
 * (the middle row, read from the left) that the paytable gives that tier, so what the reels show always matches the odds:
 *   4 netherite                                      Mythic
 *   3 netherite, then iron or diamond                Cursed (the near miss)
 *   4 diamonds, or 3 diamonds then anything           Rare (60% of Rare rolls)
 *   2 netherite, then iron or diamond, then anything  Rare (40%)
 *   anything else                                    Common
 * Two netherite in a row slow the third reel down, three slow the fourth further (the client does that from the result).
 * The top and bottom rows are random and pay nothing.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitSlots {
    public static final int IRON = 0;
    public static final int DIAMOND = 1;
    public static final int NETHERITE = 2;
    public static final int REELS = 4;
    private static final int[] TIER_WEIGHTS = {52, 33, 12, 3};
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> TRAIT_TOKEN = ITEMS.register("trait_token", TraitTokenItem::new);
    /** The trait each player has rolled and not yet placed or thrown away. */
    private static final Map<UUID, Trait> OFFERS = new HashMap<>();

    private TraitSlots() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** What the reels' middle row pays. */
    public static Trait.Tier pays(int[] line) {
        if (line[0] == NETHERITE && line[1] == NETHERITE) {
            if (line[2] == NETHERITE) {
                return line[3] == NETHERITE ? Trait.Tier.MYTHIC : Trait.Tier.CURSED;
            }
            return Trait.Tier.RARE;
        }
        if (line[0] == DIAMOND && line[1] == DIAMOND && line[2] == DIAMOND) {
            return Trait.Tier.RARE;
        }
        return Trait.Tier.COMMON;
    }

    static Trait.Tier rollTier(RandomSource random) {
        int n = random.nextInt(Arrays.stream(TIER_WEIGHTS).sum());
        for (Trait.Tier tier : Trait.Tier.values()) {
            n -= TIER_WEIGHTS[tier.ordinal()];
            if (n < 0) {
                return tier;
            }
        }
        return Trait.Tier.COMMON;
    }

    /** A middle row that pays `tier`. */
    static int[] line(Trait.Tier tier, RandomSource random) {
        int[] line = new int[REELS];
        switch (tier) {
            case MYTHIC -> Arrays.fill(line, NETHERITE);
            case CURSED -> {
                Arrays.fill(line, NETHERITE);
                line[3] = random.nextInt(2);
            }
            case RARE -> {
                if (random.nextFloat() < 0.6F) {
                    line = new int[] {DIAMOND, DIAMOND, DIAMOND, random.nextInt(3)};
                } else {
                    line = new int[] {NETHERITE, NETHERITE, random.nextInt(2), random.nextInt(3)};
                }
            }
            default -> {
                do {
                    for (int i = 0; i < REELS; i++) {
                        line[i] = random.nextInt(3);
                    }
                } while (pays(line) != Trait.Tier.COMMON);
            }
        }
        return line;
    }

    /** Rolls a trait and opens the reels on the player's screen. */
    static void spin(ServerPlayer player) {
        RandomSource random = player.getRandom();
        Trait.Tier tier = rollTier(random);
        Trait trait = pick(player, tier);
        if (trait == null) {
            player.sendSystemMessage(Component.translatable("trait.mdvlcraft.none_left").withStyle(ChatFormatting.GRAY));
            return;
        }
        int[] grid = new int[3 * REELS];
        int[] line = line(trait.tier, random);
        for (int reel = 0; reel < REELS; reel++) {
            grid[reel] = random.nextInt(3);
            grid[REELS + reel] = line[reel];
            grid[2 * REELS + reel] = random.nextInt(3);
        }
        OFFERS.put(player.getUUID(), trait);
        List<String> current = new ArrayList<>();
        for (int slot = 0; slot < Traits.slots(player); slot++) {
            Trait there = Traits.inSlot(player, slot);
            current.add(there == null ? "" : there.id());
        }
        BinderNetwork.sendTo(player, new OpenTraitSlotsPacket(grid, trait.id(), current));
    }

    /** A trait of the tier the player doesn't have (any tier if they have them all). */
    @Nullable
    private static Trait pick(ServerPlayer player, Trait.Tier tier) {
        List<Trait> owned = Traits.get(player);
        List<Trait> pool = Arrays.stream(Trait.values()).filter(t -> t.tier == tier && !owned.contains(t)).toList();
        if (pool.isEmpty()) {
            pool = Arrays.stream(Trait.values()).filter(t -> !owned.contains(t)).toList();
        }
        return pool.isEmpty() ? null : pool.get(player.getRandom().nextInt(pool.size()));
    }

    /** TraitChoicePacket: the rolled trait goes in `slot` (0-based), or is thrown away (-1). */
    public static void choose(ServerPlayer player, int slot) {
        Trait trait = OFFERS.remove(player.getUUID());
        if (trait == null) {
            return;
        }
        if (slot < 0 || slot >= Traits.slots(player)) {
            player.sendSystemMessage(Component.translatable("trait.mdvlcraft.discarded").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int other = 0; other < Traits.MAX_SLOTS; other++) {
            if (other != slot && Traits.inSlot(player, other) == trait) {
                Traits.set(player, other, null);
            }
        }
        Traits.set(player, slot, trait);
        player.sendSystemMessage(Component.translatable("trait.mdvlcraft.taken",
            Component.translatable(trait.nameKey()).withStyle(trait.tier.colour)).withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        OFFERS.remove(event.getEntity().getUUID());
    }

    /** The Trait Token: using it spins the reels. Not obtainable in survival yet (unobtainable_items.txt hides it from JEI). */
    public static class TraitTokenItem extends Item {
        public TraitTokenItem() {
            super(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                spin(serverPlayer);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.getCooldowns().addCooldown(this, 40);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.trait_token.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
