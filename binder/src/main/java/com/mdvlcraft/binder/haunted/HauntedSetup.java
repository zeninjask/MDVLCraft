package com.mdvlcraft.binder.haunted;

import com.blackgear.vanillabackport.common.level.entities.creaking.Creaking;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The Haunted Heart and the Haunted Creaking it summons. */
public final class HauntedSetup {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "mdvlcraft");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mdvlcraft");
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");

    public static final RegistryObject<Block> HAUNTED_HEART = BLOCKS.register("haunted_heart", HauntedHeartBlock::new);
    public static final RegistryObject<Item> HAUNTED_HEART_ITEM = ITEMS.register("haunted_heart", () -> new BlockItem(HAUNTED_HEART.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<HauntedHeartBlockEntity>> HAUNTED_HEART_ENTITY = BLOCK_ENTITIES.register("haunted_heart",
        () -> BlockEntityType.Builder.of(HauntedHeartBlockEntity::new, HAUNTED_HEART.get()).build(null));
    public static final RegistryObject<EntityType<HauntedCreaking>> HAUNTED_CREAKING = ENTITY_TYPES.register("haunted_creaking",
        () -> Builder.of(HauntedCreaking::new, MobCategory.MONSTER).sized(0.9F, 2.7F).clientTrackingRange(8).build("haunted_creaking"));

    private HauntedSetup() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(HauntedSetup::onAttributes);
        modBus.addListener(HauntedSetup::onCreativeTab);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(HAUNTED_HEART_ITEM);
        }
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HAUNTED_CREAKING.get(), Creaking.createAttributes().build());
    }
}
