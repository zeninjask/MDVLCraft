package com.mdvlcraft.binder.content;

import cursedfate.item.InventoryCursedSpiritItemItem;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * The Curios belt slot holds a lantern, a soul lantern or Cursed Fate's Inventory Cursed Spirit and nothing else
 * (mixin/curios/CuriosImplMixinHooksMixin; the validator lists the slot in their tooltips). A worn lantern lights up its
 * wearer like a held one (mixin/dynamiclights/LanternBeltLightMixin); a worn spirit opens its 9-slot storage on the
 * Activate Belt key (client/AbilityKeys) and is drawn at the hip (client/BeltSpiritRenderer).
 */
public final class LanternBelt {
    public static final String SLOT = "belt";
    private static final ResourceLocation VALIDATOR = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "lantern");
    public static final ResourceLocation SPIRIT = ResourceLocation.fromNamespaceAndPath("cursedfate", "inventory_cursed_spirit_item");

    private LanternBelt() {
    }

    public static void register() {
        CuriosApi.registerCurioPredicate(VALIDATOR, result -> fits(result.stack()));
    }

    public static boolean fits(ItemStack stack) {
        return stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN) || spirit(stack);
    }

    public static boolean spirit(ItemStack stack) {
        return SPIRIT.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** Activate Belt: opens the worn spirit's storage, as Cursed Fate does from its own accessory slot. */
    public static void activate(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).resolve()
            .flatMap(inventory -> inventory.getStacksHandler(SLOT))
            .ifPresent(handler -> {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (spirit(stacks.getStackInSlot(i))) {
                        InventoryCursedSpiritItemItem.openCursedInventory(player, stacks.getStackInSlot(i));
                        return;
                    }
                }
            });
    }

    public static boolean wearsLantern(LivingEntity entity) {
        return wears(entity, LanternBelt::fits);
    }

    /** A soul lantern on the belt keeps Haunted Creakings off (haunted/HauntedCreaking). */
    public static boolean wearsSoulLantern(LivingEntity entity) {
        return wears(entity, stack -> stack.is(Items.SOUL_LANTERN));
    }

    private static boolean wears(LivingEntity entity, Predicate<ItemStack> lantern) {
        return CuriosApi.getCuriosInventory(entity).resolve()
            .flatMap(inventory -> inventory.getStacksHandler(SLOT))
            .map(handler -> {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (lantern.test(stacks.getStackInSlot(i))) {
                        return true;
                    }
                }

                return false;
            })
            .orElse(false);
    }
}
