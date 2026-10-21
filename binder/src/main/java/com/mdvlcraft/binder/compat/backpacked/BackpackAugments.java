package com.mdvlcraft.binder.compat.backpacked;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.FilterableItems;
import com.mrcrayfish.backpacked.common.ItemCollection;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import com.mrcrayfish.backpacked.core.ModRegistries;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The pack's own Backpacked augments, chosen in a backpack's augment bays like the mod's: Tool Swapper (ToolSwapper),
 * Void (VoidJunk) and Lantern (mixin/dynamiclights/LanternBeltLightMixin). Their names and descriptions are the
 * augment.mdvlcraft.* lang keys and their icons are drawn by client/BackpackAugmentIcons.
 */
@RegistryContainer
public final class BackpackAugments {
    public static final AugmentType<ToolSwapper> TOOL_SWAPPER_TYPE = unit("tool_swapper", ToolSwapper.INSTANCE);
    public static final AugmentType<VoidJunk> VOID_TYPE = new AugmentType<>(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "void"),
        VoidJunk.CODEC, VoidJunk::encode, VoidJunk::decode, () -> VoidJunk.DEFAULT);
    public static final AugmentType<Lantern> LANTERN_TYPE = unit("lantern", Lantern.INSTANCE);

    public static final RegistryEntry<AugmentType<ToolSwapper>> TOOL_SWAPPER = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, TOOL_SWAPPER_TYPE.id(), () -> TOOL_SWAPPER_TYPE);
    public static final RegistryEntry<AugmentType<VoidJunk>> VOID = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, VOID_TYPE.id(), () -> VOID_TYPE);
    public static final RegistryEntry<AugmentType<Lantern>> LANTERN = RegistryEntry.custom(ModRegistries.AUGMENT_TYPES, LANTERN_TYPE.id(), () -> LANTERN_TYPE);

    private BackpackAugments() {
    }

    /** Whether any backpack the player wears has this augment in an enabled bay; works on either side. */
    public static <T extends Augment<T>> boolean wears(Player player, AugmentType<T> type) {
        return !worn(player, type).isEmpty();
    }

    /** This augment from every worn backpack that has it in an enabled bay. */
    public static <T extends Augment<T>> List<T> worn(Player player, AugmentType<T> type) {
        List<T> found = new ArrayList<>();
        for (ItemStack backpack : BackpackHelper.getBackpacks(player)) {
            T augment = backpack.isEmpty() ? null : BackpackHelper.findAugment(backpack, type);
            if (augment != null) {
                found.add(augment);
            }
        }

        return found;
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

    /**
     * Destroys picked-up items in its filter, edited in the augment's settings (client/VoidFilterMenu). New Void
     * augments start with common junk.
     */
    public record VoidJunk(ItemCollection filters) implements Augment<VoidJunk>, FilterableItems<VoidJunk> {
        public static final VoidJunk DEFAULT = new VoidJunk(new ItemCollection(new HashSet<>(Stream.of(
            "cobblestone", "cobbled_deepslate", "dirt", "gravel", "netherrack", "diorite", "andesite", "granite", "tuff", "rotten_flesh"
        ).map(name -> ResourceLocation.fromNamespaceAndPath("minecraft", name)).toList())));
        public static final Codec<VoidJunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemCollection.CODEC.fieldOf("filters").forGetter(VoidJunk::filters)
        ).apply(instance, VoidJunk::new));

        @Override
        public AugmentType<VoidJunk> type() {
            return VOID_TYPE;
        }

        @Override
        public VoidJunk addItemFilter(Item item) {
            return new VoidJunk(filters.add(item));
        }

        @Override
        public VoidJunk removeItemFilter(Item item) {
            return new VoidJunk(filters.remove(item));
        }

        @Override
        public boolean isFilteringItem(Item item) {
            return filters.has(item);
        }

        @Override
        public boolean isFilterFull() {
            return filters.ids().size() >= ItemCollection.MAX_ENTRIES;
        }

        private static void encode(FriendlyByteBuf buf, VoidJunk augment) {
            augment.filters.encode(buf);
        }

        private static VoidJunk decode(FriendlyByteBuf buf) {
            return new VoidJunk(ItemCollection.decode(buf));
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
