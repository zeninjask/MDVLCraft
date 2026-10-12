package com.mdvlcraft.binder.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OriginIcon {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> ORIGIN_ICON = ITEMS.register("origin_icon", () -> new Item(new Properties().stacksTo(1)));

    private OriginIcon() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
