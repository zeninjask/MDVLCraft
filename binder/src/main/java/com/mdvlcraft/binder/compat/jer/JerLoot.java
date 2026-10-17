package com.mdvlcraft.binder.compat.jer;

import com.mdvlcraft.binder.loot.ClientLootViews;
import com.mdvlcraft.binder.loot.DropView;
import com.mdvlcraft.binder.loot.LootView;
import com.mdvlcraft.binder.mixin.jer.DungeonEntryAccessor;
import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jeresources.api.conditionals.Conditional;
import jeresources.api.conditionals.LightLevel;
import jeresources.api.drop.LootDrop;
import jeresources.compatibility.CompatBase;
import jeresources.entry.DungeonEntry;
import jeresources.entry.MobEntry;
import jeresources.registry.DungeonRegistry;
import jeresources.registry.MobRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.VillagerTradingManager;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Fills JER's mob and chest pages from the server's loot views (loot/LootViews). Left alone, JER reads the mod jars' loot
 * tables on a multiplayer client, so it misses KubeJS data, tag entries, global loot modifiers and the pack's loot rules,
 * and it only lists vanilla chests.
 */
public final class JerLoot {
    /** What JER gets instead of its own copy of the mod jars' loot tables, which nothing needs any more. */
    public static final LootDataManager NO_LOOT_TABLES = new LootDataManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> SOURCES = new HashMap<>();
    private static Villager trader;

    private JerLoot() {
    }

    public static void registerMobs() {
        Level level = CompatBase.getLevel();

        for (LootView view : ClientLootViews.views()) {
            if (view.kind() == LootView.Kind.MOB) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(view.id());
                Supplier<LivingEntity> entity = () -> (LivingEntity)type.create(level);
                LootDrop[] drops = view.drops().stream().map(JerLoot::drop).toArray(LootDrop[]::new);
                String[] biomes = Arrays.stream(view.biomes()).mapToObj(index -> ClientLootViews.biome(index).toLanguageKey("biome")).toArray(String[]::new);
                MobRegistry.getInstance().registerMob(biomes.length == 0 ? MobEntry.create(entity, drops) : MobEntry.create(entity, (LightLevel)null, biomes, drops));
            }
        }
    }

    /**
     * Each chest is registered under its loot table id with a short title (JER's own name for a vanilla chest it knows,
     * otherwise the table's path in words) and its mod's name, which mixin/jer/DungeonWrapperMixin shows with the stacks.
     */
    public static void registerChests() {
        SOURCES.clear();

        for (LootView view : ClientLootViews.views()) {
            if (view.kind() == LootView.Kind.CHEST) {
                String name = view.id().toString();
                boolean known = view.id().getNamespace().equals("minecraft") && DungeonRegistry.categoryToLocalKeyMap.containsKey(view.id().getPath());
                if (known) {
                    name = view.id().getPath();
                } else {
                    DungeonRegistry.categoryToLocalKeyMap.put(name, chestTitle(view.id()));
                    SOURCES.put(name, view.source());
                }

                DungeonEntry entry = new DungeonEntry(name, LootTable.EMPTY);
                DungeonEntryAccessor accessor = (DungeonEntryAccessor)entry;
                view.drops().forEach(drop -> accessor.mdvlcraft$drops().add(drop(drop)));
                accessor.mdvlcraft$setMinStacks(view.minStacks());
                accessor.mdvlcraft$setMaxStacks(view.maxStacks());
                DungeonRegistry.getInstance().registerDungeonEntry(entry);
            }
        }
    }

    /** The mod a chest page belongs to, by the name it's registered under; empty for JER's vanilla chests. */
    public static String chestSource(String name) {
        return SOURCES.getOrDefault(name, "");
    }

    /**
     * Forge adds modded villager and wandering trader trades when a server starts, so a multiplayer client only has the
     * vanilla ones; JER reads trades on the client.
     */
    public static void loadTrades() {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            Method load = ObfuscationReflectionHelper.findMethod(VillagerTradingManager.class, "loadTrades", ServerAboutToStartEvent.class);

            try {
                load.invoke(null, (Object)null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Couldn't load Forge's villager trades for JER", e);
            }
        }
    }

    /** The villager JER's trades are offered by, in the client's level. */
    public static Villager trader() {
        Level level = CompatBase.getLevel();
        if (trader == null || trader.level() != level) {
            trader = EntityType.VILLAGER.create(level);
        }

        return trader;
    }

    public static void brokenTrade(ItemListing listing, RuntimeException e) {
        LOGGER.warn("JER: a {} trade can't be shown on the client: {}", listing.getClass().getName(), e.toString());
    }

    private static LootDrop drop(DropView view) {
        List<Conditional> conditionals = new ArrayList<>();
        if (view.has(DropView.PLAYER_KILL)) {
            conditionals.add(Conditional.playerKill);
        }

        if (view.has(DropView.LOOTING)) {
            conditionals.add(Conditional.affectedByLooting);
        }

        if (view.has(DropView.OTHER_DIMENSION)) {
            conditionals.add(Conditional.inDimension);
        }

        return new LootDrop(view.stack().copyWithCount(Math.max(view.min(), 1)), view.min(), view.max(), view.chance(), conditionals.toArray(Conditional[]::new));
    }

    /** "cataclysm:chests/frosted_prison_treasure" is "Frosted Prison Treasure"; folders other than chests are kept. */
    private static String chestTitle(ResourceLocation table) {
        return Arrays.stream(table.getPath().split("/")).filter(part -> !part.equals("chests")).map(JerLoot::words).collect(Collectors.joining(" - "));
    }

    private static String words(String id) {
        return Arrays.stream(id.split("_"))
            .filter(word -> !word.isEmpty())
            .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
            .collect(Collectors.joining(" "));
    }
}
