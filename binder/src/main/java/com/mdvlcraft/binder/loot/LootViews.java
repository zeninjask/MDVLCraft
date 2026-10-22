package com.mdvlcraft.binder.loot;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.LootViewsPacket;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Every living mob's drops and every chest's loot, read on the server (where KubeJS data, global loot modifiers and the
 * pack's loot rules are) and sent to each player before recipes and tags, so JER shows them in multiplayer too. Built
 * when the server starts and again on /reload.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class LootViews {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VIEWS_PER_PACKET = 64;
    /** Chests are rolled without a position, so exploration maps don't search the world for a structure. */
    private static final LootContextParamSet CHEST_VIEW = LootContextParamSet.builder().optional(LootContextParams.THIS_ENTITY).build();
    private static List<ResourceLocation> biomes;
    private static List<LootView> views;

    private LootViews() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        build(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            build(event.getPlayerList().getServer());
            event.getPlayerList().getPlayers().forEach(LootViews::send);
        } else {
            send(event.getPlayer());
        }
    }

    private static void send(ServerPlayer player) {
        List<LootView> views = views();
        for (int start = 0; start == 0 || start < views.size(); start += VIEWS_PER_PACKET) {
            int end = Math.min(start + VIEWS_PER_PACKET, views.size());
            BinderNetwork.sendTo(player, new LootViewsPacket(start == 0, end == views.size(), start == 0 ? biomes : List.of(), views.subList(start, end)));
        }
    }

    /** Every mob and chest view, for command/ObtainabilityReport. */
    public static List<LootView> views() {
        if (views == null) {
            throw new IllegalStateException("Loot views are built when the server starts");
        }

        return views;
    }

    /** A chest, barrel or vault's loot: a table declared as chest loot or kept in a chests folder; mods' test tables aren't. */
    private static boolean isChestTable(MinecraftServer server, ResourceLocation id) {
        String path = id.getPath();
        boolean chestType = server.getLootData().getLootTable(id).getParamSet() == LootContextParamSets.CHEST;
        return (chestType || path.startsWith("chests/") || path.contains("/chests/")) && !path.startsWith("test/");
    }

    private static void build(MinecraftServer server) {
        CodeDrops.check();
        long started = System.nanoTime();
        ServerLevel overworld = server.overworld();
        List<ServerLevel> levels = new ArrayList<>(List.of(overworld));
        server.getAllLevels().forEach(level -> {
            if (level != overworld) {
                levels.add(level);
            }
        });
        FakePlayer looter = FakePlayerFactory.getMinecraft(overworld);
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceLocation> biomeIds = List.copyOf(new TreeSet<>(biomeRegistry.keySet()));
        Map<EntityType<?>, TreeSet<Integer>> spawns = new HashMap<>();

        for (int i = 0; i < biomeIds.size(); i++) {
            Biome biome = biomeRegistry.get(biomeIds.get(i));

            for (MobCategory category : MobCategory.values()) {
                for (SpawnerData spawn : biome.getMobSettings().getMobs(category).unwrap()) {
                    spawns.computeIfAbsent(spawn.type, type -> new TreeSet<>()).add(i);
                }
            }
        }

        List<LootView> built = new ArrayList<>();
        int mobs = 0;

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (!type.isEnabled(overworld.enabledFeatures())) {
                continue;
            }

            Entity entity;
            try {
                entity = type.create(overworld);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.debug("JER: {} can't be created on the server, so its drops aren't shown: {}", id, e.toString());
                continue;
            }

            if (entity instanceof LivingEntity living) {
                try {
                    LootDrops drops = LootDrops.of(living.getLootTable(), levels, level -> mobParams(level, living, looter));
                    int[] biomes = spawns.getOrDefault(type, new TreeSet<>()).stream().mapToInt(Integer::intValue).toArray();
                    List<DropView> all = new ArrayList<>(drops.drops());
                    all.addAll(CodeDrops.of(id));
                    built.add(new LootView(LootView.Kind.MOB, id, modName(id), 0, 0, biomes, all));
                    mobs++;
                } catch (RuntimeException e) {
                    LOGGER.warn("JER: the drops of {} couldn't be read", id, e);
                }
            }
        }

        List<ResourceLocation> chests = server.getLootData().getKeys(LootDataType.TABLE).stream().filter(table -> isChestTable(server, table)).sorted().toList();

        for (ResourceLocation table : chests) {
            try {
                LootDrops drops = LootDrops.of(table, List.of(overworld), level -> chestParams(level, looter));
                built.add(new LootView(LootView.Kind.CHEST, table, modName(table), drops.minStacks(), drops.maxStacks(), new int[0], drops.drops()));
            } catch (RuntimeException e) {
                LOGGER.warn("JER: the loot of {} couldn't be read", table, e);
            }
        }

        biomes = biomeIds;
        views = List.copyOf(built);
        LOGGER.info("JER loot views: {} mobs and {} chests in {} ms", mobs, built.size() - mobs, (System.nanoTime() - started) / 1000000L);
    }

    /** The display name of the mod an id belongs to, or its namespace when no mod has it (a datapack). */
    private static String modName(ResourceLocation id) {
        return ModList.get().getModContainerById(id.getNamespace()).map(container -> container.getModInfo().getDisplayName()).orElse(id.getNamespace());
    }

    private static LootParams mobParams(ServerLevel level, LivingEntity mob, FakePlayer looter) {
        return new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, mob)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(level.getSharedSpawnPos()))
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(looter))
            .withParameter(LootContextParams.KILLER_ENTITY, looter)
            .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, looter)
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, looter)
            .create(LootContextParamSets.ENTITY);
    }

    private static LootParams chestParams(ServerLevel level, FakePlayer looter) {
        return new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, looter).create(CHEST_VIEW);
    }
}
