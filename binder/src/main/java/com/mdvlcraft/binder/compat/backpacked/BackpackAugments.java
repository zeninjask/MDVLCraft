package com.mdvlcraft.binder.compat.backpacked;

import com.mojang.serialization.Codec;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.core.ModRegistries;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The pack's own Backpacked augments, chosen in a backpack's augment bays like the mod's: Tool Swapper (ToolSwapper),
 * Void (VoidJunk) and Lantern (mixin/dynamiclights/LanternBeltLightMixin). Their names and descriptions are the
 * augment.mdvlcraft.* lang keys and their icons are drawn by client/BackpackAugmentIcons.
 */
@RegistryContainer
public final class BackpackAugments {
    public static final AugmentType<ToolSwapper> TOOL_SWAPPER_TYPE = unit("tool_swapper", ToolSwapper.INSTANCE);
    public static final AugmentType<VoidJunk> VOID_TYPE = unit("void", VoidJunk.INSTANCE);
    public static final AugmentType<Lantern> LANTERN_TYPE = unit("lantern", Lantern.INSTANCE);

    public static final RegistryEntry<AugmentType<ToolSwapper>> TOOL_SWAPPER = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, TOOL_SWAPPER_TYPE.id(), () -> TOOL_SWAPPER_TYPE);
    public static final RegistryEntry<AugmentType<VoidJunk>> VOID = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, VOID_TYPE.id(), () -> VOID_TYPE);
    public static final RegistryEntry<AugmentType<Lantern>> LANTERN = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, LANTERN_TYPE.id(), () -> LANTERN_TYPE);

    private BackpackAugments() {
    }

    /** Whether any backpack the player wears has this augment in an enabled bay; works on either side. */
    public static <T extends Augment<T>> boolean wears(Player player, AugmentType<T> type) {
        for (ItemStack backpack : BackpackHelper.getBackpacks(player)) {
            if (!backpack.isEmpty() && BackpackHelper.findAugment(backpack, type) != null) {
                return true;
            }
        }

        return false;
    }

    private static <T extends Augment<T>> AugmentType<T> unit(String name, T instance) {
        return new AugmentType<>(ResourceLocation.fromNamespaceAndPath("mdvlcraft", name), Codec.unit(instance), Augment.noOp(), Augment.unit(instance), () -> instance);
    }

    public record ToolSwapper() implements Augment<ToolSwapper> {
        public static final ToolSwapper INSTANCE = new ToolSwapper();

        @Override
        public AugmentType<ToolSwapper> type() {
            return TOOL_SWAPPER_TYPE;
        }
    }

    public record VoidJunk() implements Augment<VoidJunk> {
        public static final VoidJunk INSTANCE = new VoidJunk();

        @Override
        public AugmentType<VoidJunk> type() {
            return VOID_TYPE;
        }
    }

    public record Lantern() implements Augment<Lantern> {
        public static final Lantern INSTANCE = new Lantern();

        @Override
        public AugmentType<Lantern> type() {
            return LANTERN_TYPE;
        }
    }
}
