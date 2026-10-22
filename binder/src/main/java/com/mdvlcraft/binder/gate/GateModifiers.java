package com.mdvlcraft.binder.gate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Gate modifiers (plan 9.1, late-game challenge 1). A and S keys roll modifiers when they are crafted (A 0–1, S 1–3; a key
 * that reaches an inventory any other way, such as from loot, rolls on its first inventory tick), kept in the key's NBT
 * list {@link #NBT} and shown in its tooltip. Using the Gate Forge with such a key in hand rerolls it for {@link #REROLL_COST}
 * A essence stones. Attuning a key keeps its modifiers. Opening a gate with the key hands them to the new instance (kept in
 * memory: every gate closes on restart), and a gate that turns red keeps them.
 *
 * Each modifier is a data entry, data/mdvlcraft/gate_modifiers/<id>.json: {"effect": ..., "value": ..., "weight": ...}.
 * The effect is one of the coded kinds below, so a new modifier can reuse one with another value; its name and description
 * come from the lang keys gate.mdvlcraft.modifier.<id>(.desc). Effects:
 *   mob_damage   gate mobs deal +value damage (Enraged 0.3)
 *   mob_count    +value mobs per room (Swarming 0.5): every 1/value-th fresh normal mob gets a copy at its spawn point
 *   no_regen     no natural (food) regeneration inside (Cursed)
 *   death_burst  mobs explode on death with power value, breaking no blocks (Volatile)
 *   mana_cost    spells cost value times the mana (Silenced; Iron's Spells' mana change event)
 *   room_elite   every combat room without an elite gets one (Elite Pack)
 *   red_chance   red chance times value (Crimson; RedGates.roll)
 *   mob_armour   gate mobs +value armour (Ironclad)
 * Swarming and Elite Pack work from the spawn (EntityJoinLevelEvent) rather than inside GateCombat, so the combat code needs
 * no change. Each modifier adds REWARD_BONUS to essence stones and crystal veins and EXPERIENCE_BONUS to the clear bonus.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateModifiers {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** NBT list of modifier ids on a key; present (maybe empty) once the key has rolled. */
    public static final String NBT = "modifiers";
    public static final int REROLL_COST = 4;
    static final double REWARD_BONUS = 0.20;
    static final double EXPERIENCE_BONUS = 0.15;
    private static final UUID ARMOUR = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0201");

    /** One data entry. */
    public record Modifier(String id, String effect, double value, int weight) {
    }

    /** Loaded entries by id, in file order. */
    static final Map<String, Modifier> REGISTRY = new LinkedHashMap<>();
    /** Modifier ids per live instance. */
    private static final Map<Integer, List<String>> ACTIVE = new HashMap<>();
    /** Swarming: fresh normal mobs counted per instance, so every 1/value-th gets a copy. */
    private static final Map<Integer, Integer> SWARM_COUNT = new HashMap<>();
    /** Elite Pack: rooms already given (or checked for) their elite, per instance. */
    private static final Map<Integer, Set<Integer>> ELITE_ROOMS = new HashMap<>();
    /** Spawns to make next tick (spawning from inside the join event is not safe). */
    private static final ArrayDeque<Pending> PENDING = new ArrayDeque<>();
    /** True while this class spawns, so its own spawns aren't counted again. */
    private static boolean spawning;

    private record Pending(ResourceLocation level, int instance, EntityType<?> type, BlockPos pos, int room, boolean elite) {
    }

    private GateModifiers() {
    }

    // --- data --------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "gate_modifiers") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
                REGISTRY.clear();
                files.forEach((id, json) -> {
                    try {
                        JsonObject o = json.getAsJsonObject();
                        REGISTRY.put(id.getPath(), new Modifier(id.getPath(), GsonHelper.getAsString(o, "effect"), GsonHelper.getAsDouble(o, "value", 0.0),
                            GsonHelper.getAsInt(o, "weight", 1)));
                    } catch (RuntimeException e) {
                        LOGGER.warn("Bad gate modifier {}: {}", id, e.getMessage());
                    }
                });
                LOGGER.info("Loaded {} gate modifiers", REGISTRY.size());
            }
        });
    }

    // --- keys --------------------------------------------------------------------------------------------------------

    /** How many modifiers a key of this rank rolls: A 0–1, S 1–3, none below. */
    static int rollCount(GateRank rank, RandomSource random) {
        return switch (rank) {
            case A -> random.nextInt(2);
            case S -> 1 + random.nextInt(3);
            default -> 0;
        };
    }

    /** Distinct ids drawn by weight. */
    static List<String> roll(GateRank rank, RandomSource random) {
        List<Modifier> pool = new ArrayList<>(REGISTRY.values());
        List<String> picked = new ArrayList<>();
        for (int n = rollCount(rank, random); n > 0 && !pool.isEmpty(); n--) {
            int total = pool.stream().mapToInt(m -> Math.max(0, m.weight())).sum();
            if (total <= 0) {
                break;
            }
            int r = random.nextInt(total);
            for (Modifier m : pool) {
                r -= Math.max(0, m.weight());
                if (r < 0) {
                    picked.add(m.id());
                    pool.remove(m);
                    break;
                }
            }
        }
        return picked;
    }

    static boolean rolled(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(NBT, Tag.TAG_LIST);
    }

    public static List<String> of(ItemStack stack) {
        List<String> ids = new ArrayList<>();
        if (stack.getTag() != null) {
            stack.getTag().getList(NBT, Tag.TAG_STRING).forEach(t -> ids.add(t.getAsString()));
        }
        return ids;
    }

    static void set(ItemStack stack, List<String> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id)));
        stack.getOrCreateTag().put(NBT, list);
    }

    /** GateKeyItem.inventoryTick: a key of A or S that has never rolled rolls now (server side; needs the data loaded). */
    public static void ensureRolled(ItemStack stack, GateRank rank, Level level) {
        if (!level.isClientSide && (rank == GateRank.A || rank == GateRank.S) && !rolled(stack) && !REGISTRY.isEmpty()) {
            set(stack, roll(rank, level.random));
        }
    }

    /** GateKeyItem.onCraftedBy: an attuned key keeps the modifiers of the key in the grid; a new key rolls. */
    public static void onCrafted(ItemStack stack, GateRank rank, Level level, Player player) {
        if (level.isClientSide || rolled(stack)) {
            return;
        }
        for (Slot slot : player.containerMenu.slots) {
            ItemStack input = slot.getItem();
            if (slot.container instanceof CraftingContainer && input != stack && input.getItem() == stack.getItem() && rolled(input)) {
                set(stack, of(input));
                return;
            }
        }
        ensureRolled(stack, rank, level);
    }

    /** Tooltip lines: each modifier with its effect, and the reward bonus. */
    public static void tooltip(ItemStack stack, List<Component> tooltip) {
        List<String> ids = of(stack);
        for (String id : ids) {
            tooltip.add(Component.translatableWithFallback("gate.mdvlcraft.modifier." + id, id).withStyle(ChatFormatting.RED)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatableWithFallback("gate.mdvlcraft.modifier." + id + ".desc", "").withStyle(ChatFormatting.GRAY)));
        }
        if (!ids.isEmpty()) {
            tooltip.add(Component.translatableWithFallback("gate.mdvlcraft.modifier.bonus", "Rewards +%s%%, clear experience +%s%%",
                Math.round(ids.size() * REWARD_BONUS * 100), Math.round(ids.size() * EXPERIENCE_BONUS * 100)).withStyle(ChatFormatting.GOLD));
        }
    }

    /** GateForgeBlock.use with an A or S key in hand: rerolls it for REROLL_COST A essence stones (true: handled). */
    public static boolean reroll(Player player, InteractionHand hand) {
        ItemStack key = player.getItemInHand(hand);
        if (!(key.getItem() instanceof GateKeyItem item) || (item.rank() != GateRank.A && item.rank() != GateRank.S)) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        ItemStack stone = new ItemStack(GateItems.ESSENCE_STONES.get(GateRank.A).get());
        int have = player.getInventory().countItem(stone.getItem());
        if (!player.getAbilities().instabuild && have < REROLL_COST) {
            player.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.modifier.reroll_cost",
                "Rerolling needs %s A essence stones", REROLL_COST), true);
            return true;
        }
        if (REGISTRY.isEmpty()) {
            return true;
        }
        if (!player.getAbilities().instabuild) {
            player.getInventory().clearOrCountMatchingItems(s -> s.is(stone.getItem()), REROLL_COST, player.inventoryMenu.getCraftSlots());
        }
        if (key.getCount() > 1) {   // reroll one key off the stack
            ItemStack one = key.split(1);
            set(one, roll(item.rank(), player.getRandom()));
            if (!player.getInventory().add(one)) {
                player.drop(one, false);
            }
        } else {
            set(key, roll(item.rank(), player.getRandom()));
        }
        player.playNotifySound(SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    // --- instances ---------------------------------------------------------------------------------------------------

    /** GateKeyItem, after the gate opened: the key's modifiers go to the instance. */
    public static void attach(int instanceId, ItemStack key) {
        attach(instanceId, of(key));
    }

    static void attach(int instanceId, List<String> ids) {
        if (ids.isEmpty()) {
            ACTIVE.remove(instanceId);
        } else {
            ACTIVE.put(instanceId, List.copyOf(ids));
        }
    }

    /** RedGates.convert: the red instance keeps the old one's modifiers (and trial rule). */
    public static void carry(int from, int to) {
        List<String> ids = ACTIVE.remove(from);
        if (ids != null) {
            ACTIVE.put(to, ids);
        }
        RuleGates.carry(from, to);
    }

    public static List<String> ids(int instanceId) {
        return ACTIVE.getOrDefault(instanceId, List.of());
    }

    /** The value of the instance's first modifier with this effect, or `none`. */
    static double value(int instanceId, String effect, double none) {
        for (String id : ids(instanceId)) {
            Modifier m = REGISTRY.get(id);
            if (m != null && m.effect().equals(effect)) {
                return m.value();
            }
        }
        return none;
    }

    static boolean has(int instanceId, String effect) {
        return !Double.isNaN(value(instanceId, effect, Double.NaN));
    }

    /** Red chance factor (Crimson), for RedGates.roll. */
    public static double redFactor(int instanceId) {
        return value(instanceId, "red_chance", 1.0);
    }

    /** Essence stones and crystals: +20% per modifier, x1.5 in a trial gate. */
    public static double rewardFactor(Instance instance) {
        return (1.0 + REWARD_BONUS * ids(instance.id).size()) * (RuleGates.rule(instance.id) != null ? RuleGates.REWARD_FACTOR : 1.0);
    }

    /** Clear experience: +15% per modifier, x1.5 in a trial gate. */
    public static double experienceFactor(Instance instance) {
        return (1.0 + EXPERIENCE_BONUS * ids(instance.id).size()) * (RuleGates.rule(instance.id) != null ? RuleGates.REWARD_FACTOR : 1.0);
    }

    /** `n * factor`, the fraction rounded up by chance. */
    public static int scale(int n, double factor, RandomSource random) {
        double exact = n * factor;
        int whole = (int)exact;
        return whole + (random.nextDouble() < exact - whole ? 1 : 0);
    }

    /** The live instance a gate mob belongs to, if it is in that instance's world. */
    @Nullable
    private static Instance mobInstance(Entity entity) {
        int id = GateSpawner.instanceOf(entity);
        if (id < 0 || !(entity.level() instanceof ServerLevel level) || !ACTIVE.containsKey(id)) {
            return null;
        }
        Instance instance = GateInstances.get(level.getServer()).byId(id);
        return instance != null && instance.live() && instance.dimension == level.dimension() ? instance : null;
    }

    /** The live instance a player is on and inside, if it has modifiers. */
    @Nullable
    static Instance playerInstance(Player player) {
        if (!(player instanceof ServerPlayer sp) || !GateDimensions.isGate(sp.level().dimension())) {
            return null;
        }
        Instance instance = GateInstances.get(sp.server).ofPlayer(sp.getUUID());
        return instance != null && instance.dimension == sp.level().dimension() ? instance : null;
    }

    // --- effects -----------------------------------------------------------------------------------------------------

    /** Enraged. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        Instance instance = attacker == null ? null : mobInstance(attacker);
        if (instance != null) {
            double extra = value(instance.id, "mob_damage", 0.0);
            if (extra != 0.0) {
                event.setAmount((float)(event.getAmount() * (1.0 + extra)));
            }
        }
    }

    /** Ironclad on every fresh gate mob; Swarming and Elite Pack queue their extra spawns. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Instance instance = mobInstance(mob);
        if (instance == null) {
            return;
        }
        double armour = value(instance.id, "mob_armour", 0.0);
        AttributeInstance attribute = mob.getAttribute(Attributes.ARMOR);
        if (armour != 0.0 && attribute != null && attribute.getModifier(ARMOUR) == null) {
            attribute.addPermanentModifier(new AttributeModifier(ARMOUR, "gate modifier armour", armour, AttributeModifier.Operation.ADDITION));
        }
        if (spawning) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        String role = data.getString(GateSpawner.ROLE);
        int room = data.contains(GateSpawner.ROOM) ? data.getInt(GateSpawner.ROOM) : -1;
        BlockPos home = data.contains(GateSpawner.HOME) ? BlockPos.of(data.getLong(GateSpawner.HOME)) : mob.blockPosition();
        ResourceLocation level = mob.level().dimension().location();
        double swarm = value(instance.id, "mob_count", 0.0);
        if (swarm > 0.0 && role.isEmpty()) {
            int n = SWARM_COUNT.merge(instance.id, 1, Integer::sum);
            int every = Math.max(1, (int)Math.round(1.0 / swarm));
            if (n % every == 0) {
                PENDING.add(new Pending(level, instance.id, mob.getType(), home, room, false));
            }
        }
        if (has(instance.id, "room_elite") && room >= 0 && ELITE_ROOMS.computeIfAbsent(instance.id, i -> new HashSet<>()).add(room)
            && !roomHas(instance, room, "spawn:elite") && !roomHas(instance, room, "boss")) {
            EntityType<?> elite = GateMobs.pick(instance.theme, instance.rank, GateMobs.Kind.ELITE, mob.getRandom());
            PENDING.add(new Pending(level, instance.id, elite != null ? elite : mob.getType(), home, room, true));
        }
    }

    private static boolean roomHas(Instance instance, int room, String kind) {
        for (Marker marker : instance.markers) {
            if (marker.room() == room && marker.kind().equals(kind)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        GateInstances gates = GateInstances.get(server);
        spawning = true;
        try {
            while (!PENDING.isEmpty()) {
                Pending p = PENDING.poll();
                Instance instance = gates.byId(p.instance());
                ServerLevel level = instance == null ? null : server.getLevel(instance.dimension);
                if (level != null && instance.live() && level.dimension().location().equals(p.level())) {
                    GateSpawner.spawn(level, instance, p.type(), p.pos(), p.elite() ? GateSpawner.ELITE : null, p.room());
                }
            }
        } finally {
            spawning = false;
        }
        // forget closed instances
        ACTIVE.keySet().removeIf(id -> gates.byId(id) == null);
        SWARM_COUNT.keySet().removeIf(id -> !ACTIVE.containsKey(id));
        ELITE_ROOMS.keySet().removeIf(id -> !ACTIVE.containsKey(id));
    }

    /** Volatile: a burst that hurts but breaks nothing. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Instance instance = dead instanceof Mob ? mobInstance(dead) : null;
        if (instance != null) {
            double power = value(instance.id, "death_burst", 0.0);
            if (power > 0.0) {
                dead.level().explode(dead, dead.getX(), dead.getY() + dead.getBbHeight() / 2.0, dead.getZ(), (float)power, Level.ExplosionInteraction.NONE);
            }
        }
    }

    /** Cursed: food regeneration is cancelled inside. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player) {
            Instance instance = playerInstance(player);
            if (instance != null && has(instance.id, "no_regen") && RuleGates.fromFood()) {
                event.setCanceled(true);
            }
        }
    }

    /** Silenced: mana spent inside costs `value` times as much. */
    @SubscribeEvent
    public static void onMana(ChangeManaEvent event) {
        float spent = event.getOldMana() - event.getNewMana();
        if (spent <= 0.0F) {
            return;
        }
        Instance instance = playerInstance(event.getEntity());
        double factor = instance == null ? 1.0 : value(instance.id, "mana_cost", 1.0);
        if (factor != 1.0) {
            event.setNewMana(Math.max(0.0F, event.getOldMana() - (float)(spent * factor)));
        }
    }
}
