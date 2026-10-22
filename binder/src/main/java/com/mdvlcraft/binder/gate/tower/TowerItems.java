package com.mdvlcraft.binder.gate.tower;

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
 * The Demon Castle's items and its gate (plan 10.1, 10.4): the Castle Sigil (crafted from black and S essence stones and
 * netherite; opens the castle gate and is kept), Tower Seals (the tower-only token from boss floors) and the castle gate
 * entity the sigil places.
 */
public final class TowerItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");

    public static final RegistryObject<Item> CASTLE_SIGIL = ITEMS.register("castle_sigil", CastleSigilItem::new);
    public static final RegistryObject<Item> TOWER_SEAL = ITEMS.register("tower_seal", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
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

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CASTLE_SIGIL);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(TOWER_SEAL);
        }
    }
}
