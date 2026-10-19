package com.mdvlcraft.binder.content;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * The Curios belt slot holds a lantern or a soul lantern and nothing else (mixin/curios/CuriosImplMixinHooksMixin; the lantern validator
 * lists the slot in the lantern's tooltip), and a worn lantern lights up its wearer like a held one (mixin/dynamiclights/LanternBeltLightMixin).
 */
public final class LanternBelt {
    public static final String SLOT = "belt";
    private static final ResourceLocation VALIDATOR = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "lantern");

    private LanternBelt() {
    }

    public static void register() {
        CuriosApi.registerCurioPredicate(VALIDATOR, result -> fits(result.stack()));
    }

    public static boolean fits(ItemStack stack) {
        return stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
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
