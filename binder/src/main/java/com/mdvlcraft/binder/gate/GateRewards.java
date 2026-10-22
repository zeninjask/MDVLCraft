package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Gate rewards (plan section 6): essence stones from gate mobs, mana crystal veins at the dungeon's crystal markers, the
 * per-player boss chest and the clear-bonus skill experience. Also registers the reward blocks and items (veins, chest,
 * Gate Forge, Return Stone, tempering runes).
 *
 * Amounts are tuned so one clear yields about what the next key costs (4 stones + 3-4 crystals): a normal mob drops a
 * stone 15% of the time, an elite 1-2, the boss 2-3, a vein holds 3-4 ore blocks (one crystal each before Fortune), and
 * the boss chest adds 0-2 of each per player. Red gates give 1.5x stones, double veins and black stones from the boss.
 *
 * The dungeon builder calls {@link #placeCrystals} after building; the spawner calls {@link #onCleared} when the boss dies.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateRewards {
    /** Persistent NBT string on a gate mob: "elite" or "boss" (absent for normal mobs). Set by the spawner. */
    public static final String ROLE = "mdvlcraft_gate_role";
    /** Clear-bonus skill experience by rank (plan 6.2), E to S. */
    private static final int[] CLEAR_EXPERIENCE = {200, 400, 800, 1500, 3000, 6000};

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "mdvlcraft");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mdvlcraft");

    public static final Map<GateRank, RegistryObject<Block>> CRYSTAL_ORES = new EnumMap<>(GateRank.class);
    public static final Map<GateRank, RegistryObject<Item>> TEMPER_RUNES = new EnumMap<>(GateRank.class);
    public static final RegistryObject<Block> REWARD_CHEST = BLOCKS.register("gate_reward_chest", GateRewardChestBlock::new);
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<GateRewardChestBlockEntity>> REWARD_CHEST_ENTITY = BLOCK_ENTITIES.register("gate_reward_chest",
        () -> BlockEntityType.Builder.of(GateRewardChestBlockEntity::new, REWARD_CHEST.get()).build(null));
    public static final RegistryObject<Block> FORGE = BLOCKS.register("gate_forge", GateForgeBlock::new);
    public static final RegistryObject<Item> FORGE_ITEM = ITEMS.register("gate_forge", () -> new BlockItem(FORGE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RETURN_STONE = ITEMS.register("return_stone", ReturnStoneItem::new);

    /** Instances whose clear has been rewarded (all instances close on restart, so this needn't be saved). */
    private static final Set<Integer> REWARDED = new HashSet<>();

    static {
        for (GateRank rank : GateRank.values()) {
            String key = rank.name().toLowerCase(Locale.ROOT);
            RegistryObject<Block> ore = BLOCKS.register("mana_crystal_ore_" + key, () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(3.0F + rank.ordinal(), 3.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                .lightLevel(state -> 7), UniformInt.of(2 + rank.ordinal(), 5 + rank.ordinal() * 2)));
            CRYSTAL_ORES.put(rank, ore);
            ITEMS.register("mana_crystal_ore_" + key, () -> new BlockItem(ore.get(), new Item.Properties().rarity(GateItems.rarity(rank))));
            TEMPER_RUNES.put(rank, ITEMS.register("temper_rune_" + key, () -> new TemperRuneItem(rank)));
        }
    }

    private GateRewards() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(GateRewards::onCreativeTab);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            CRYSTAL_ORES.values().forEach(ore -> event.accept(ore.get()));
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FORGE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RETURN_STONE);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            TEMPER_RUNES.values().forEach(event::accept);
        }
    }

    /** Whether players may mine this block inside a gate (GateProtection's one exception). */
    public static boolean isVein(BlockState state) {
        for (RegistryObject<Block> ore : CRYSTAL_ORES.values()) {
            if (state.is(ore.get())) {
                return true;
            }
        }
        return false;
    }

    /** The mob's role from {@link #ROLE}; pack bosses (#forge:bosses) count as bosses even untagged. */
    static String role(LivingEntity mob) {
        String role = mob.getPersistentData().getString(ROLE);
        return role.isEmpty() && mob.getType().is(Tags.EntityTypes.BOSSES) ? "boss" : role;
    }

    /** Gate mobs drop essence stones of their gate's rank. Only inside their own gate's world, so break mobs (which carry
     * the tag out in the overworld) drop none. The boss's own loot is replaced by the stones and the boss chest. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        int id = GateSpawner.instanceOf(mob);
        if (id < 0 || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Instance instance = GateInstances.get(level.getServer()).byId(id);
        if (instance == null || instance.dimension != level.dimension()) {
            return;
        }
        RandomSource random = mob.getRandom();
        String role = role(mob);
        int stones = switch (role) {
            case "boss" -> 2 + random.nextInt(2);
            case "elite" -> 1 + random.nextInt(2);
            default -> random.nextFloat() < 0.15F ? 1 : 0;
        };
        if (instance.red) {
            stones = scaled(stones, 1.5, random);
        }
        if (role.equals("boss")) {
            event.getDrops().clear();
            if (instance.red) {
                drop(event, mob, new ItemStack(GateItems.BLACK_ESSENCE_STONE.get(), 1 + random.nextInt(2)));
            }
        }
        if (stones > 0) {
            drop(event, mob, new ItemStack(GateItems.ESSENCE_STONES.get(instance.rank).get(), stones));
        }
    }

    /** `n * factor`, the fraction rounded up by chance. */
    private static int scaled(int n, double factor, RandomSource random) {
        double exact = n * factor;
        int whole = (int)exact;
        return whole + (random.nextDouble() < exact - whole ? 1 : 0);
    }

    private static void drop(LivingDropsEvent event, LivingEntity mob, ItemStack stack) {
        ItemEntity item = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), stack);
        item.setDefaultPickUpDelay();
        event.getDrops().add(item);
    }

    /**
     * Puts a mana crystal vein of the instance's rank at each `crystal` marker: 3-4 ore blocks (6-8 in a red gate) grown
     * into the air around the marker, each touching a block, so mining one never opens a wall. Call after building.
     */
    public static void placeCrystals(ServerLevel level, Instance instance) {
        BlockState ore = CRYSTAL_ORES.get(instance.rank).get().defaultBlockState();
        RandomSource random = level.random;
        for (Marker marker : instance.markers) {
            if (marker.kind().equals("crystal")) {
                int size = (3 + random.nextInt(2)) * (instance.red ? 2 : 1);
                vein(level, marker.pos(), ore, size, random);
            }
        }
    }

    static void vein(ServerLevel level, BlockPos centre, BlockState ore, int size, RandomSource random) {
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 2, 2))) {
            if (level.getBlockState(pos).isAir() && attached(level, pos)) {
                spots.add(pos.immutable());
            }
        }
        // nearest first, with some jitter so veins aren't all the same shape
        spots.sort((a, b) -> Double.compare(a.distSqr(centre) + random.nextDouble() * 2.0, b.distSqr(centre) + random.nextDouble() * 2.0));
        for (int i = 0; i < Math.min(size, spots.size()); i++) {
            level.setBlock(spots.get(i), ore, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean attached(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The boss died and the instance is CLEARED: every roster member inside the cell gets the clear-bonus skill experience
     * and their own roll in the boss chest, which appears at the `chest` marker (else the boss marker, else the entry).
     * Runs once per instance.
     */
    public static void onCleared(ServerLevel level, Instance instance) {
        if (!REWARDED.add(instance.id)) {
            return;
        }
        List<ServerPlayer> present = level.players().stream()
            .filter(player -> instance.roster.contains(player.getUUID()) && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(player.position()))
            .toList();
        int experience = CLEAR_EXPERIENCE[instance.rank.ordinal()] * (instance.red ? 3 : 2) / 2;
        present.forEach(player -> giveExperience(player, experience));

        BlockPos pos = chestPos(instance);
        level.setBlockAndUpdate(pos, REWARD_CHEST.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof GateRewardChestBlockEntity chest) {
            ResourceLocation table = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gates/" + instance.rank.name().toLowerCase(Locale.ROOT) + "/boss");
            present.forEach(player -> chest.roll(player, table));
        }
        present.forEach(player -> player.sendSystemMessage(Component.translatable("gate.mdvlcraft.cleared", experience)));
    }

    private static BlockPos chestPos(Instance instance) {
        Function<String, BlockPos> find = kind -> instance.markers.stream().filter(m -> m.kind().equals(kind)).map(Marker::pos).findFirst().orElse(null);
        BlockPos chest = find.apply("chest");
        if (chest != null) {
            return chest;
        }
        BlockPos boss = find.apply("boss");
        return boss != null ? boss.north(2) : instance.entry.north(2);
    }

    /** Skill experience in every skill tab the player has unlocked, as a kill gives it. */
    static void giveExperience(ServerPlayer player, int amount) {
        SkillsAPI.streamUnlockedCategories(player).forEach(category -> category.getExperience().ifPresent(e -> e.addTotal(player, amount)));
    }
}
