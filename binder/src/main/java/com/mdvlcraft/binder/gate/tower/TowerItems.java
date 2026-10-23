package com.mdvlcraft.binder.gate.tower;

import com.mdvlcraft.binder.gate.GateItems;
import com.mdvlcraft.binder.gate.GateRank;
import com.mdvlcraft.binder.gate.GateRewards;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The Demon Castle's items and its gate (plan 10.1, 10.4): the Castle Sigil (crafted from black and S essence stones and
 * netherite; opens the castle gate and is kept), Tower Seals (the tower-only token from boss floors) and the castle gate
 * entity the sigil places. Seals are spent at the Gate Forge ({@link #forgeOffers}): on the top tempering tier (an S Temper
 * Rune without the six S stones) and on the Castle Trophy, a cosmetic.
 */
public final class TowerItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");

    public static final RegistryObject<Item> CASTLE_SIGIL = ITEMS.register("castle_sigil", CastleSigilItem::new);
    public static final RegistryObject<Item> TOWER_SEAL = ITEMS.register("tower_seal", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    /** Cosmetic: bought with Tower Seals. */
    public static final RegistryObject<Item> CASTLE_TROPHY = ITEMS.register("castle_trophy",
        () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant()));
    static final int RUNE_SEALS = 6;
    static final int TROPHY_SEALS = 16;
    /** Sized like an S gate; CastleGateEntity sets its own size. */
    public static final RegistryObject<EntityType<CastleGateEntity>> CASTLE_GATE = ENTITY_TYPES.register("castle_gate",
        () -> EntityType.Builder.<CastleGateEntity>of(CastleGateEntity::new, MobCategory.MISC).sized(5.4F, 9.0F).fireImmune().clientTrackingRange(10)
            .updateInterval(20).build("castle_gate"));

    private TowerItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(TowerItems::onCreativeTab);
    }

    /** BinderNetwork.register: the sigil menu's packets and the status line's, from message id `id` on. */
    public static void registerPackets(SimpleChannel channel, int id) {
        channel.registerMessage(id++, CastleMenuPacket.class, CastleMenuPacket::encode, CastleMenuPacket::decode, CastleMenuPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(id++, CastleStartPacket.class, CastleStartPacket::encode, CastleStartPacket::decode, CastleStartPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id, TowerStatusPacket.class, TowerStatusPacket::encode, TowerStatusPacket::decode, TowerStatusPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** GateForgeBlock.offers: the seal trades (fixed, unlimited, like the forge's own). */
    public static List<MerchantOffer> forgeOffers() {
        return List.of(
            offer(new ItemStack(TOWER_SEAL.get(), RUNE_SEALS), new ItemStack(GateItems.MANA_CRYSTALS.get(GateRank.S).get(), 2),
                new ItemStack(GateRewards.TEMPER_RUNES.get(GateRank.S).get())),
            offer(new ItemStack(TOWER_SEAL.get(), TROPHY_SEALS), ItemStack.EMPTY, new ItemStack(CASTLE_TROPHY.get())));
    }

    private static MerchantOffer offer(ItemStack costA, ItemStack costB, ItemStack result) {
        return new MerchantOffer(costA, costB, result, Integer.MAX_VALUE, 0, 0.0F);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CASTLE_SIGIL);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(TOWER_SEAL);
            event.accept(CASTLE_TROPHY);
        }
    }
}
