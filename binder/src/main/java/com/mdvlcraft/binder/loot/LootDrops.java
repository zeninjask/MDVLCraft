package com.mdvlcraft.binder.loot;

import com.mdvlcraft.binder.content.DisabledContent;
import com.mdvlcraft.binder.content.RuneSwap;
import com.mdvlcraft.binder.mixin.loot.CompositeEntryBaseAccessor;
import com.mdvlcraft.binder.mixin.loot.ForgeInternalHandlerAccessor;
import com.mdvlcraft.binder.mixin.loot.LootItemAccessor;
import com.mdvlcraft.binder.mixin.loot.LootItemRandomChanceConditionAccessor;
import com.mdvlcraft.binder.mixin.loot.LootItemRandomChanceWithLootingConditionAccessor;
import com.mdvlcraft.binder.mixin.loot.LootModifierAccessor;
import com.mdvlcraft.binder.mixin.loot.LootPoolAccessor;
import com.mdvlcraft.binder.mixin.loot.LootPoolEntryContainerAccessor;
import com.mdvlcraft.binder.mixin.loot.LootPoolSingletonContainerAccessor;
import com.mdvlcraft.binder.mixin.loot.LootTableAccessor;
import com.mdvlcraft.binder.mixin.loot.LootTableReferenceAccessor;
import com.mdvlcraft.binder.mixin.loot.SetItemCountFunctionAccessor;
import com.mdvlcraft.binder.mixin.loot.TagEntryAccessor;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.slf4j.Logger;

/**
 * What a loot table gives, the way JER shows it: each item with its count range, its chance and the conditions it needs.
 * <p>
 * The table itself is read entry by entry (items, tags, nested tables, alternatives and groups), so rare drops keep their
 * exact chance. Global loot modifiers can do anything, so each one whose conditions pass is run many times on an empty
 * list and what it adds is counted. Last, the pack's own loot rules apply as they do in ForgeHooksMixin: removed runes
 * become their crafting item and forbidden items are gone.
 */
public final class LootDrops {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Class<?>> WARNED = Collections.synchronizedSet(new HashSet<>());
    /** How often a modifier's conditions are tried; random-chance conditions need more than one try. */
    private static final int CONDITION_TRIES = 32;
    /** How often a modifier that passes is run; an item it adds 1 time in 100 shows with 99% certainty. */
    private static final int MODIFIER_SAMPLES = 512;
    /** How often an entry type this class doesn't know is rolled. */
    private static final int ENTRY_SAMPLES = 64;
    private static final int NUMBER_SAMPLES = 100;
    private final LootDataManager data;
    private final LootContext context;
    private final Map<Item, Drop> drops = new LinkedHashMap<>();
    private final Set<ResourceLocation> visiting = new HashSet<>();
    private int minStacks;
    private int maxStacks;

    private LootDrops(LootDataManager data, LootContext context) {
        this.data = data;
        this.context = context;
    }

    /**
     * @param params the loot parameters in a level: the first level is where the table is normally rolled, and a modifier
     *               that passes only in one of the others is marked {@link DropView#OTHER_DIMENSION}
     */
    public static LootDrops of(ResourceLocation tableId, List<ServerLevel> levels, Function<ServerLevel, LootParams> params) {
        ServerLevel home = levels.get(0);
        LootDrops drops = new LootDrops(home.getServer().getLootData(), context(tableId, params.apply(home)));
        drops.table(tableId, 1.0F, 0, List.of(), true);
        drops.modifiers(tableId, levels, params);
        drops.applyPackRules();
        return drops;
    }

    /** The table alone, without global loot modifiers: command/ObtainabilityReport reads every table, blocks' included. */
    public static LootDrops ofTable(ResourceLocation tableId, ServerLevel level, LootParams params) {
        LootDrops drops = new LootDrops(level.getServer().getLootData(), context(tableId, params));
        drops.table(tableId, 1.0F, 0, List.of(), true);
        drops.applyPackRules();
        return drops;
    }

    public List<DropView> drops() {
        return this.drops.values().stream().map(drop -> new DropView(drop.stack, drop.min, drop.max, drop.chance, drop.flags)).toList();
    }

    public int minStacks() {
        return this.minStacks;
    }

    public int maxStacks() {
        return this.maxStacks;
    }

    private static LootContext context(ResourceLocation tableId, LootParams params) {
        LootContext context = new LootContext.Builder(params).withOptionalRandomSeed(tableId.hashCode()).create(null);
        context.setQueriedLootTableId(tableId);
        return context;
    }

    private void table(ResourceLocation id, float chance, int flags, List<LootItemFunction> outer, boolean top) {
        if (!this.visiting.add(id)) {
            return;
        }

        LootTable table = this.data.getLootTable(id);
        List<LootItemFunction> functions = concat(((LootTableAccessor)table).mdvlcraft$functions(), outer);

        for (LootPool pool : ((LootTableAccessor)table).mdvlcraft$pools()) {
            if (top) {
                this.minStacks += this.low(pool.getRolls());
                this.maxStacks += this.high(pool.getRolls()) + this.high(pool.getBonusRolls());
            }

            this.pool((LootPoolAccessor)pool, this.average(pool.getRolls()), chance, flags, functions);
        }

        this.visiting.remove(id);
    }

    private void pool(LootPoolAccessor pool, float rolls, float chance, int flags, List<LootItemFunction> outer) {
        float poolChance = chance * chanceOf(pool.mdvlcraft$conditions());
        int poolFlags = flags | flagsOf(pool.mdvlcraft$conditions());
        List<Leaf> leaves = new ArrayList<>();

        for (LootPoolEntryContainer entry : pool.mdvlcraft$entries()) {
            expand(entry, 1.0F, 0, true, leaves);
        }

        int total = leaves.stream().filter(Leaf::counts).mapToInt(Leaf::weight).sum();
        if (total == 0) {
            return;
        }

        List<LootItemFunction> functions = concat(pool.mdvlcraft$functions(), outer);

        for (Leaf leaf : leaves) {
            float perRoll = leaf.chance * leaf.weight / total;
            float any = poolChance * (1.0F - (float)Math.pow(1.0F - perRoll, rolls));
            this.give(leaf, any, poolFlags | leaf.flags, concat(((LootPoolSingletonContainerAccessor)leaf.entry).mdvlcraft$functions(), functions));
        }
    }

    /** Flattens alternatives and groups into the single entries a roll picks from; only an alternative's first child adds weight. */
    private static void expand(LootPoolEntryContainer entry, float chance, int flags, boolean counts, List<Leaf> out) {
        LootItemCondition[] conditions = ((LootPoolEntryContainerAccessor)entry).mdvlcraft$conditions();
        float entryChance = chance * chanceOf(conditions);
        int entryFlags = flags | flagsOf(conditions);
        if (entry instanceof CompositeEntryBase) {
            LootPoolEntryContainer[] children = ((CompositeEntryBaseAccessor)entry).mdvlcraft$children();
            boolean alternatives = entry instanceof AlternativesEntry;

            for (int i = 0; i < children.length; i++) {
                expand(children[i], entryChance, entryFlags, counts && (!alternatives || i == 0), out);
            }
        } else if (entry instanceof LootPoolSingletonContainer single) {
            int weight = ((LootPoolSingletonContainerAccessor)single).mdvlcraft$weight();
            if (entry instanceof TagEntry tag && ((TagEntryAccessor)tag).mdvlcraft$expand()) {
                for (Item item : tagItems(tag)) {
                    out.add(new Leaf(single, item, weight, counts, entryChance, entryFlags));
                }
            } else {
                out.add(new Leaf(single, null, weight, counts, entryChance, entryFlags));
            }
        } else {
            warnOnce(entry.getClass(), "loot entry type not read for JER and the item audit");
        }
    }

    private void give(Leaf leaf, float chance, int flags, List<LootItemFunction> functions) {
        LootPoolSingletonContainer entry = leaf.entry;
        if (leaf.item != null) {
            this.item(leaf.item, chance, flags, functions);
        } else if (entry instanceof LootItem item) {
            this.item(((LootItemAccessor)item).mdvlcraft$item(), chance, flags, functions);
        } else if (entry instanceof TagEntry tag) {
            tagItems(tag).forEach(item -> this.item(item, chance, flags, functions));
        } else if (entry instanceof LootTableReference reference) {
            this.table(((LootTableReferenceAccessor)reference).mdvlcraft$name(), chance, flags, functions, false);
        } else if (!(entry instanceof EmptyLootItem)) {
            Set<Item> rolled = new HashSet<>();

            for (int i = 0; i < ENTRY_SAMPLES; i++) {
                ((LootPoolSingletonContainerAccessor)entry).mdvlcraft$createItemStack(stack -> rolled.add(stack.getItem()), this.context);
            }

            rolled.forEach(item -> this.item(item, chance, flags, functions));
        }
    }

    private static List<Item> tagItems(TagEntry tag) {
        List<Item> items = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(((TagEntryAccessor)tag).mdvlcraft$tag()).forEach(holder -> items.add(holder.value()));
        return items;
    }

    /** Entry functions run first, then the pool's, then the table's, as in a real roll; counts are read, the rest are applied. */
    private void item(Item item, float chance, int flags, List<LootItemFunction> functions) {
        ItemStack stack = new ItemStack(item);
        int min = 1;
        int max = 1;
        int itemFlags = flags;

        for (LootItemFunction function : functions) {
            if (function instanceof SetItemCountFunction count) {
                NumberProvider value = ((SetItemCountFunctionAccessor)count).mdvlcraft$value();
                if (((SetItemCountFunctionAccessor)count).mdvlcraft$add()) {
                    min += this.low(value);
                    max += this.high(value);
                } else {
                    min = this.low(value);
                    max = this.high(value);
                }
            } else if (function instanceof LootingEnchantFunction) {
                itemFlags |= DropView.LOOTING;
            } else if (!(function instanceof ApplyBonusCount)) {
                stack = this.apply(function, stack);
            }
        }

        if (!stack.isEmpty() && max > 0) {
            this.add(stack, Math.max(min, 0), max, chance, itemFlags);
        }
    }

    private ItemStack apply(LootItemFunction function, ItemStack stack) {
        try {
            return function.apply(stack.copy(), this.context);
        } catch (RuntimeException e) {
            warnOnce(function.getClass(), "loot function failed outside a real roll (" + e + "); its item is shown without it");
            return stack;
        }
    }

    private void modifiers(ResourceLocation tableId, List<ServerLevel> levels, Function<ServerLevel, LootParams> params) {
        Map<ServerLevel, LootContext> contexts = new HashMap<>();
        contexts.put(levels.get(0), this.context);

        for (IGlobalLootModifier modifier : ForgeInternalHandlerAccessor.mdvlcraft$lootModifierManager().getAllLootMods()) {
            for (int i = 0; i < levels.size(); i++) {
                LootContext context = contexts.computeIfAbsent(levels.get(i), level -> context(tableId, params.apply(level)));
                if (passes(modifier, context, i == 0 ? CONDITION_TRIES : 1)) {
                    this.sample(modifier, context, i == 0 ? 0 : DropView.OTHER_DIMENSION);
                    break;
                }
            }
        }
    }

    private static boolean passes(IGlobalLootModifier modifier, LootContext context, int tries) {
        if (!(modifier instanceof LootModifier)) {
            return true;
        }

        LootItemCondition[] conditions = ((LootModifierAccessor)modifier).mdvlcraft$conditions();
        return IntStream.range(0, tries).anyMatch(i -> Stream.of(conditions).allMatch(condition -> condition.test(context)));
    }

    private void sample(IGlobalLootModifier modifier, LootContext context, int flags) {
        Map<Item, int[]> seen = new LinkedHashMap<>();
        Map<Item, ItemStack> stacks = new HashMap<>();

        for (int i = 0; i < MODIFIER_SAMPLES; i++) {
            ObjectArrayList<ItemStack> added;
            try {
                added = modifier.apply(new ObjectArrayList<>(), context);
            } catch (RuntimeException e) {
                warnOnce(modifier.getClass(), "global loot modifier failed on an empty roll (" + e + "); not shown in JER");
                return;
            }

            Map<Item, Integer> counts = new HashMap<>();
            added.forEach(stack -> {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                stacks.putIfAbsent(stack.getItem(), stack.copyWithCount(1));
            });
            counts.forEach((item, count) -> {
                int[] stats = seen.computeIfAbsent(item, key -> new int[]{0, Integer.MAX_VALUE, 0});
                stats[0]++;
                stats[1] = Math.min(stats[1], count);
                stats[2] = Math.max(stats[2], count);
            });
        }

        seen.forEach((item, stats) -> this.add(stacks.get(item), stats[1], stats[2], (float)stats[0] / MODIFIER_SAMPLES, flags));
    }

    /** ForgeHooksMixin's rules, so JER shows what a real roll gives. */
    private void applyPackRules() {
        List<Drop> before = new ArrayList<>(this.drops.values());
        this.drops.clear();

        for (Drop drop : before) {
            ItemStack shown = RuneSwap.swap(drop.stack);
            if (!DisabledContent.isForbidden(shown)) {
                this.add(shown, drop.min, drop.max, drop.chance, drop.flags);
            }
        }
    }

    /** The same item from two places: either may drop it; it needs a player kill or another dimension only if both do. */
    private void add(ItemStack stack, int min, int max, float chance, int flags) {
        Drop drop = this.drops.get(stack.getItem());
        if (drop == null) {
            this.drops.put(stack.getItem(), new Drop(stack.copyWithCount(1), min, max, Math.min(chance, 1.0F), flags));
        } else {
            drop.min = Math.min(drop.min, min);
            drop.max = Math.max(drop.max, max);
            drop.chance = 1.0F - (1.0F - drop.chance) * (1.0F - Math.min(chance, 1.0F));
            drop.flags = drop.flags & flags | (drop.flags | flags) & DropView.LOOTING;
        }
    }

    private static float chanceOf(LootItemCondition[] conditions) {
        float chance = 1.0F;

        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemRandomChanceCondition random) {
                chance *= ((LootItemRandomChanceConditionAccessor)random).mdvlcraft$probability();
            } else if (condition instanceof LootItemRandomChanceWithLootingCondition random) {
                chance *= ((LootItemRandomChanceWithLootingConditionAccessor)random).mdvlcraft$percent();
            }
        }

        return chance;
    }

    private static int flagsOf(LootItemCondition[] conditions) {
        int flags = 0;

        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemKilledByPlayerCondition) {
                flags |= DropView.PLAYER_KILL;
            } else if (condition instanceof LootItemRandomChanceWithLootingCondition) {
                flags |= DropView.LOOTING;
            }
        }

        return flags;
    }

    private int low(NumberProvider value) {
        return IntStream.range(0, NUMBER_SAMPLES).map(i -> value.getInt(this.context)).min().orElseThrow();
    }

    private int high(NumberProvider value) {
        return IntStream.range(0, NUMBER_SAMPLES).map(i -> value.getInt(this.context)).max().orElseThrow();
    }

    private float average(NumberProvider value) {
        return (float)IntStream.range(0, NUMBER_SAMPLES).map(i -> value.getInt(this.context)).average().orElseThrow();
    }

    private static List<LootItemFunction> concat(LootItemFunction[] first, List<LootItemFunction> rest) {
        List<LootItemFunction> all = new ArrayList<>(List.of(first));
        all.addAll(rest);
        return all;
    }

    private static void warnOnce(Class<?> type, String problem) {
        if (WARNED.add(type)) {
            LOGGER.warn("{}: {}", type.getName(), problem);
        }
    }

    /** A single entry a pool roll can pick; item is set when an expanded tag gave each of its items its own entry. */
    private record Leaf(LootPoolSingletonContainer entry, Item item, int weight, boolean counts, float chance, int flags) {
    }

    private static final class Drop {
        final ItemStack stack;
        int min;
        int max;
        float chance;
        int flags;

        Drop(ItemStack stack, int min, int max, float chance, int flags) {
            this.stack = stack;
            this.min = min;
            this.max = max;
            this.chance = chance;
            this.flags = flags;
        }
    }
}
