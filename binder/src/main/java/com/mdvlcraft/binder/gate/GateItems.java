package com.mdvlcraft.binder.gate;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Gate items and the gate entity: a key per rank (gate_key_e .. gate_key_s), the essence stones mobs drop (one per rank,
 * plus black from red gates) and the mana crystals mined from crystal veins (one per rank). Stones and crystals are the
 * next rank's key materials and the Gate Forge's currency.
 */
public final class GateItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");

    public static final Map<GateRank, RegistryObject<Item>> KEYS = new EnumMap<>(GateRank.class);
    public static final Map<GateRank, RegistryObject<Item>> ESSENCE_STONES = new EnumMap<>(GateRank.class);
    public static final Map<GateRank, RegistryObject<Item>> MANA_CRYSTALS = new EnumMap<>(GateRank.class);
    public static final RegistryObject<Item> BLACK_ESSENCE_STONE = ITEMS.register("essence_stone_black",
        () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    /** Sized for an E gate; GateEntity scales itself by rank. */
    public static final RegistryObject<EntityType<GateEntity>> GATE = ENTITY_TYPES.register("gate",
        () -> EntityType.Builder.<GateEntity>of(GateEntity::new, MobCategory.MISC).sized(2.0F, 3.0F).fireImmune().clientTrackingRange(10)
            .updateInterval(20).build("gate"));

    /** Trial gates' trophy (RuleGates), their statue (GateStatue) and the entrance room's return portal (GateReturn). */
    public static final RegistryObject<Item> TROPHY = ITEMS.register("gate_trophy", RuleGates.TrophyItem::new);
    public static final RegistryObject<EntityType<GateStatue>> STATUE = ENTITY_TYPES.register("gate_statue",
        () -> EntityType.Builder.<GateStatue>of(GateStatue::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).build("gate_statue"));
    public static final RegistryObject<EntityType<ReturnGateEntity>> RETURN_GATE = ENTITY_TYPES.register("return_gate",
        () -> EntityType.Builder.<ReturnGateEntity>of(ReturnGateEntity::new, MobCategory.MISC).sized(1.8F, 3.0F).fireImmune()
            .clientTrackingRange(10).updateInterval(20).build("return_gate"));

    static {
        for (GateRank rank : GateRank.values()) {
            String key = rank.name().toLowerCase(java.util.Locale.ROOT);
            KEYS.put(rank, ITEMS.register("gate_key_" + key, () -> new GateKeyItem(rank)));
            ESSENCE_STONES.put(rank, ITEMS.register("essence_stone_" + key, () -> new Item(new Item.Properties().rarity(rarity(rank)))));
            MANA_CRYSTALS.put(rank, ITEMS.register("mana_crystal_" + key, () -> new Item(new Item.Properties().rarity(rarity(rank)))));
        }
    }

    private GateItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(GateItems::onCreativeTab);
    }

    static Rarity rarity(GateRank rank) {
        return switch (rank) {
            case E, D -> Rarity.COMMON;
            case C, B -> Rarity.UNCOMMON;
            case A -> Rarity.RARE;
            case S -> Rarity.EPIC;
        };
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            KEYS.values().forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ESSENCE_STONES.values().forEach(event::accept);
            event.accept(BLACK_ESSENCE_STONE);
            MANA_CRYSTALS.values().forEach(event::accept);
        }
    }
}
