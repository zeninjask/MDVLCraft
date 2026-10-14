package com.mdvlcraft.binder.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * The Curios belt slot holds a lantern and nothing else (mixin/curios/CuriosImplMixinHooksMixin; the lantern validator
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
        return stack.is(Items.LANTERN);
    }

    public static boolean wearsLantern(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
            .flatMap(inventory -> inventory.getStacksHandler(SLOT))
            .map(handler -> {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (fits(stacks.getStackInSlot(i))) {
                        return true;
                    }
                }

                return false;
            })
            .orElse(false);
    }
}
