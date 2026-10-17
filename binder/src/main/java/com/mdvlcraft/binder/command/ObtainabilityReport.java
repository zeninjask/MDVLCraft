package com.mdvlcraft.binder.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.content.TradeTuning;
import com.mdvlcraft.binder.content.UnobtainableItems;
import com.mdvlcraft.binder.loot.LootDrops;
import com.mdvlcraft.binder.loot.LootView;
import com.mdvlcraft.binder.loot.LootViews;
import com.mdvlcraft.binder.mixin.loot.ForgeInternalHandlerAccessor;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Which items a survival player can get, for compat/JeiHiddenContent's list of what JEI hides. An item counts once one
 * of these gives it:
 * <ul>
 * <li>a loot table, any block's included: mods place blocks with code no data shows, so every block is taken to exist
 * (a block with no drop, like a spawner or bedrock, still gives nothing);</li>
 * <li>a global loot modifier, as JER's views sample them or naming the item;</li>
 * <li>a villager or wandering trader trade;</li>
 * <li>a container, item frame or armour stand in a structure template;</li>
 * <li>a recipe whose every ingredient can be got;</li>
 * <li>{@link #OTHER_SOURCES}, the ways only code knows about.</li>
 * </ul>
 * Written by /mdvl obtainable to obtainability.tsv; items without a source that JEI still lists are the candidates to hide.
 */
final class ObtainabilityReport {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Ways to get an item that no data shows: filling, growing, dyeing, brewing and other code. */
    static final Map<String, String> OTHER_SOURCES = Map.ofEntries(
        Map.entry("minecraft:white_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:orange_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:magenta_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:light_blue_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:yellow_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:lime_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:pink_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:gray_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:light_gray_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:cyan_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:purple_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:blue_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:brown_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:green_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:red_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:black_bundle", "dyed in a crafting grid (Vanilla Backport's bundle colouring)"),
        Map.entry("minecraft:dragon_breath", "bottled from the Ender Dragon's breath"),
        Map.entry("minecraft:powder_snow_bucket", "scooped with a bucket"),
        Map.entry("minecraft:salmon_bucket", "the mob caught in a bucket"),
        Map.entry("minecraft:sulfur_cube_bucket", "the mob caught in a bucket"),
        Map.entry("cataclysm:modern_remnant_bucket", "the mob caught in a bucket"),
        Map.entry("cataclysm:netherite_ministrosity_bucket", "the mob caught in a bucket"),
        Map.entry("cataclysm:the_baby_leviathan_bucket", "the mob caught in a bucket"),
        Map.entry("naturalist:bass_bucket", "the mob caught in a bucket"),
        Map.entry("naturalist:catfish_bucket", "the mob caught in a bucket"),
        Map.entry("naturalist:snail_bucket", "the mob caught in a bucket"),
        Map.entry("cataclysm:music_disc_the_cataclysmfarer", "Cataclysm's reward (ServerEventHandler)"),
        Map.entry("alshanex_familiars:bard_harp", "from a Mysterious Orb"),
        Map.entry("alshanex_familiars:bard_trinket", "from a Mysterious Orb"),
        Map.entry("cursedfate:blessed_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:brotherly_curse_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:brotherly_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:crow_sorcerer_fabric", "from a Cosmetic Cube"),
        Map.entry("cursedfate:cursed_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:devout_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:disgraced_zenin_fabric", "from a Cosmetic Cube"),
        Map.entry("cursedfate:fallen_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:judicial_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:lucky_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:maiden_sorceress_fabric", "from a Cosmetic Cube"),
        Map.entry("cursedfate:prestigious_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:reluctant_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:shinjuku_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:sorcerer_killer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:strongest_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:weary_sorcerer_cloth", "from a Cosmetic Cube"),
        Map.entry("cursedfate:wretched_sorcerer_fabric", "from a Cosmetic Cube"),
        Map.entry("cursedfate:chroma_orb", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:cosmetic_cube", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:crow_sorcerer_axe", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:death_painting", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:dragon_bone", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:hiten", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:inventory_cursed_spirit_item", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:inverted_spearof_heaven", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:jet_black_blade", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:kamutoke", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:naginata", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:nanamis_cleaver", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:ring_of_promises", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:slaughter_demon", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:split_soul_katana", "bought with jujutsu tokens (Cursed Fate's TradeProcedure)"),
        Map.entry("cursedfate:mummified_head", "dropped by the Shrine Spirit"),
        Map.entry("cursedfate:mutual_love_katana_weapon", "taken from the Mutual Love katana mob"),
        Map.entry("cursedfate:mutual_love_katana_jacobs_ladder_weapon", "taken from the Mutual Love katana mob"),
        Map.entry("naoyaaddon:arrogant_sorcerer_fabric", "given by Naoya and Naobito"),
        Map.entry("naoyaaddon:old_sorcerer_fabric", "given by Naoya and Naobito"),
        Map.entry("naturalist:butterfly", "caught (Naturalist)"),
        Map.entry("naturalist:caterpillar", "caught (Naturalist)"),
        Map.entry("naturalist:duck_egg", "laid by ducks"),
        Map.entry("epicfight:stray_hat", "worn by strays, dropped like any mob's armour"),
        Map.entry("epicfight:stray_pants", "worn by strays, dropped like any mob's armour"),
        Map.entry("epicfight:stray_robe", "worn by strays, dropped like any mob's armour"),
        Map.entry("irons_spellbooks:magehunter", "carried by magehunter vindicators"),
        Map.entry("irons_spellbooks:music_disc_whispers_of_ice", "sold by the Cryomancer"),
        Map.entry("gravestone:obituary", "left with your grave when you die"),
        Map.entry("farmersdelight:roast_chicken", "served from its feast block with a bowl"),
        Map.entry("farmersdelight:honey_glazed_ham", "served from its feast block with a bowl"),
        Map.entry("farmersdelight:shepherds_pie", "served from its feast block with a bowl"),
        Map.entry("farmersdelight:stuffed_pumpkin", "served from its feast block with a bowl"),
        Map.entry("farmersdelight:gleaming_salad", "served from its feast block with a bowl"),
        Map.entry("elixirum:elixir", "brewed in Elixirum's cauldron"),
        Map.entry("elixirum:extract", "brewed in Elixirum's cauldron"),
        Map.entry("elixirum:witch_totem_of_undying", "Elixirum's special recipe"),
        Map.entry("mimic:mimic_invitation", "a 1% chance when Herobrine vanishes (compat/HerobrineInvitation)"),
        Map.entry("alshanex_familiars:pet_soul", "a dead familiar's soul, picked up (SoulEntity)"),
        Map.entry("irons_spellbooks:lightning_rod", "made from an Energized Core (EnergizedCoreItem)"),
        Map.entry("magistuarmory:rustedchainmail_helmet", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustednorman_helmet", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedkettlehat", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedbarbute", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedgreathelm", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedchainmail_chestplate", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedcrusader_chestplate", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedhalfarmor_chestplate", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedchainmail_leggings", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedchainmail_boots", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rustedcrusader_boots", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rusted_bastardsword", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:rusted_heavymace", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)"),
        Map.entry("magistuarmory:corruptedroundshield", "worn by overworld zombies and skeletons (Epic Knights' mob equipment config)")
    );
    private static final Set<String> RESULT_KEYS = Set.of("result", "results", "output", "outputs", "byproduct");
    private static final int TRADE_TRIES = 32;
    /** A template bigger than this (Legendary Monsters' whole space station) is listed as a problem rather than read. */
    private static final long TEMPLATE_BYTES = 256L * 1024L * 1024L;
    private static final LootContextParamSet VIEW = LootContextParamSet.builder().optional(LootContextParams.THIS_ENTITY).build();
    private final MinecraftServer server;
    private final ServerLevel overworld;
    private final Map<Item, String> sources = new LinkedHashMap<>();
    private final Map<ResourceLocation, Set<Item>> recipeOutputs = new HashMap<>();
    private final List<String> problems = new ArrayList<>();

    private ObtainabilityReport(MinecraftServer server) {
        this.server = server;
        this.overworld = server.overworld();
    }

    static ObtainabilityReport.Result build(MinecraftServer server) {
        ObtainabilityReport report = new ObtainabilityReport(server);
        OTHER_SOURCES.forEach((id, how) -> report.add(item(id), "other: " + how));
        report.step("loot tables", report::loot);
        report.step("loot modifiers", report::modifiers);
        report.step("trades", report::trades);
        report.step("structure templates", report::structures);
        report.step("recipe files", report::recipeFiles);
        report.step("recipes", report::resolve);
        List<ObtainabilityReport.Row> rows = new ArrayList<>();
        ForgeRegistries.ITEMS.getEntries().forEach(entry -> {
            if (entry.getValue() != Items.AIR) {
                rows.add(new ObtainabilityReport.Row(entry.getKey().location(), report.sources.get(entry.getValue()), UnobtainableItems.isHidden(entry.getValue())));
            }
        });
        return new ObtainabilityReport.Result(rows, report.problems);
    }

    private void step(String name, Runnable step) {
        long started = System.nanoTime();
        step.run();
        LOGGER.info("Obtainability: {} read in {} ms, {} items with a source", name, (System.nanoTime() - started) / 1000000L, this.sources.size());
    }

    private static Item item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("OTHER_SOURCES names an unregistered item: " + id);
        }

        return item;
    }

    private boolean add(Item item, String source) {
        return item != Items.AIR && this.sources.putIfAbsent(item, source) == null;
    }

    private void loot() {
        LootParams params = new LootParams.Builder(this.overworld)
            .withParameter(LootContextParams.THIS_ENTITY, FakePlayerFactory.getMinecraft(this.overworld))
            .create(VIEW);

        for (ResourceLocation table : this.server.getLootData().getKeys(LootDataType.TABLE)) {
            try {
                LootDrops.ofTable(table, this.overworld, params).drops().forEach(drop -> this.add(drop.stack().getItem(), "loot " + table));
            } catch (RuntimeException e) {
                this.problems.add("loot table " + table + " not read: " + e);
            }
        }

        for (LootView view : LootViews.views()) {
            view.drops().forEach(drop -> this.add(drop.stack().getItem(), "loot modifier on " + view.id()));
        }
    }

    @SuppressWarnings("unchecked")
    private void modifiers() {
        for (IGlobalLootModifier modifier : ForgeInternalHandlerAccessor.mdvlcraft$lootModifierManager().getAllLootMods()) {
            Codec<IGlobalLootModifier> codec = (Codec<IGlobalLootModifier>)modifier.codec();
            codec.encodeStart(JsonOps.INSTANCE, modifier)
                .result()
                .ifPresentOrElse(
                    json -> strings(json, text -> this.itemNamed(text, "named by loot modifier " + modifier.getClass().getSimpleName())),
                    () -> this.problems.add("loot modifier " + modifier.getClass().getName() + " couldn't be written out")
                );
        }
    }

    private void itemNamed(String text, String source) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
            this.add(ForgeRegistries.ITEMS.getValue(id), source);
        }
    }

    private void trades() {
        RandomSource random = RandomSource.create(1L);
        Villager villager = EntityType.VILLAGER.create(this.overworld);

        for (Map.Entry<VillagerProfession, Int2ObjectMap<ItemListing[]>> entry : VillagerTrades.TRADES.entrySet()) {
            villager.setVillagerData(villager.getVillagerData().setProfession(entry.getKey()));
            for (ItemListing[] listings : entry.getValue().values()) {
                for (ItemListing listing : listings) {
                    this.offers(listing, villager, random, "trade " + ForgeRegistries.VILLAGER_PROFESSIONS.getKey(entry.getKey()));
                }
            }
        }

        WanderingTrader trader = EntityType.WANDERING_TRADER.create(this.overworld);
        for (ItemListing[] listings : VillagerTrades.WANDERING_TRADER_TRADES.values()) {
            for (ItemListing listing : listings) {
                this.offers(listing, trader, random, "wandering trader");
            }
        }
    }

    /** Map trades search the world for a structure on every offer; they sell a filled map. */
    private void offers(ItemListing listing, AbstractVillager trader, RandomSource random, String source) {
        ItemListing inner = listing;
        while (inner instanceof TradeTuning.Tuned tuned) {
            inner = tuned.listing();
        }

        if (inner.getClass().getName().contains("Map")) {
            this.add(Items.FILLED_MAP, source);
            this.problems.add("map trade not rolled: " + inner.getClass().getName());
            return;
        }

        for (int i = 0; i < TRADE_TRIES; i++) {
            MerchantOffer offer = listing.getOffer(trader, random);
            if (offer != null) {
                this.add(offer.getResult().getItem(), source);
            }
        }
    }

    /** Items in a structure's chests, item frames and armour stands. */
    private void structures() {
        Map<ResourceLocation, Resource> files = this.server.getResourceManager().listResources("structures", path -> path.getPath().endsWith(".nbt"));
        files.forEach((path, resource) -> {
            CompoundTag saved;
            try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(resource.open())))) {
                saved = NbtIo.read(stream, new NbtAccounter(TEMPLATE_BYTES));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (RuntimeException e) {
                this.problems.add("structure " + path + " not read: " + e.getMessage());
                return;
            }

            nbtIds(saved.get("blocks"), text -> this.itemNamed(text, "structure " + path));
            nbtIds(saved.get("entities"), text -> this.itemNamed(text, "structure " + path));
        });
    }

    /**
     * Outputs a recipe's own file lists that getResultItem doesn't give (a cutting board's byproduct), and the outputs of
     * recipes a mod loads itself (Iron's alchemist cauldron, rituals), which the recipe manager never sees; a vanilla-loaded
     * recipe missing from the recipe manager was removed (by a condition or content/DisabledContent) and doesn't count.
     */
    private void recipeFiles() {
        Map<ResourceLocation, Resource> files = this.server.getResourceManager().listResources("recipes", path -> path.getPath().endsWith(".json"));
        files.forEach((path, resource) -> {
            String file = path.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(path.getNamespace(), file.substring("recipes/".length(), file.length() - ".json".length()));
            JsonElement json;
            try (Reader reader = resource.openAsReader()) {
                json = JsonParser.parseReader(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            Set<Item> outputs = new HashSet<>();
            results(json, outputs);
            if (this.server.getRecipeManager().byKey(id).isPresent()) {
                this.recipeOutputs.put(id, outputs);
            } else if (json.isJsonObject() && json.getAsJsonObject().has("type")) {
                ResourceLocation type = ResourceLocation.tryParse(json.getAsJsonObject().get("type").getAsString());
                if (type != null && !ForgeRegistries.RECIPE_SERIALIZERS.containsKey(type)) {
                    outputs.forEach(item -> this.add(item, "mod recipe " + id));
                }
            }
        });
    }

    private static void results(JsonElement json, Set<Item> outputs) {
        if (json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(entry -> {
                if (RESULT_KEYS.contains(entry.getKey())) {
                    strings(entry.getValue(), text -> {
                        ResourceLocation id = ResourceLocation.tryParse(text);
                        if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                            outputs.add(ForgeRegistries.ITEMS.getValue(id));
                        }
                    });
                } else {
                    results(entry.getValue(), outputs);
                }
            });
        } else if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> results(element, outputs));
        }
    }

    /** Recipes whose ingredients can all be got give their outputs, until nothing changes. */
    private void resolve() {
        List<Recipe<?>> recipes = new ArrayList<>(this.server.getRecipeManager().getRecipes());
        boolean changed = true;

        while (changed) {
            changed = false;
            Iterator<Recipe<?>> pending = recipes.iterator();

            while (pending.hasNext()) {
                Recipe<?> recipe = pending.next();
                if (recipe.getIngredients().stream().allMatch(this::canGet)) {
                    pending.remove();
                    String source = "recipe " + recipe.getId();
                    changed |= this.add(recipe.getResultItem(this.server.registryAccess()).getItem(), source);
                    for (Item output : this.recipeOutputs.getOrDefault(recipe.getId(), Set.of())) {
                        changed |= this.add(output, source);
                    }
                }
            }
        }
    }

    private boolean canGet(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 || Arrays.stream(options).anyMatch(stack -> this.sources.containsKey(stack.getItem()));
    }

    private static void strings(JsonElement json, Consumer<String> out) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            out.accept(json.getAsString());
        } else if (json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(entry -> strings(entry.getValue(), out));
        } else if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> strings(element, out));
        }
    }

    private static void nbtIds(Tag tag, Consumer<String> out) {
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                if (key.equals("id") && value instanceof StringTag text) {
                    out.accept(text.getAsString());
                } else {
                    nbtIds(value, out);
                }
            }
        } else if (tag instanceof ListTag list) {
            list.forEach(element -> nbtIds(element, out));
        }
    }

    /** hidden: JEI already doesn't list it. */
    record Row(ResourceLocation id, String source, boolean hidden) {
    }

    record Result(List<ObtainabilityReport.Row> rows, List<String> problems) {
        TreeSet<String> missing() {
            TreeSet<String> missing = new TreeSet<>();
            this.rows.stream().filter(row -> row.source() == null && !row.hidden()).forEach(row -> missing.add(row.id().toString()));
            return missing;
        }
    }
}
