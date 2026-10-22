package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Rule gates (plan 9.3, late-game challenge 4): when an S gate's first entry doesn't turn it red, it has a CHANCE to become
 * a trial gate with one rule. The rule shows as a title whenever a roster member enters, and on a stone tablet beside the
 * entry (two chiseled stone bricks with a floating text display above). A trial gate gives REWARD_FACTOR times the essence,
 * crystals and clear experience (GateModifiers.rewardFactor) and every player present at the clear a Trial Trophy.
 * Its boss chest rolls a second time for each player with a 50% chance (extraChestRoll: x1.5 on average). Four rules:
 *   No Potions          using any potion (drinking or throwing) is cancelled
 *   No Healing          every heal is cancelled except natural regeneration from food (FoodData.tick)
 *   Statues             every room but the entrance gets a GateStatue when it triggers: it hunts the players in its room
 *                       but moves only while nobody looks at it, and can't be hurt
 *   The Narrowing Door  the way in narrows each time a member leaves before the clear (by any way: death, Return Stone,
 *                       command): whoever leaves is taken off the roster and can't come back in, and when nobody of the
 *                       roster is left inside the gate fails (GateCombat.fail). Someone must stay behind; logging out
 *                       keeps a player's place
 * Rules are kept in memory per instance: every gate closes on restart.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RuleGates {
    public enum Rule {
        NO_POTIONS("No Potions", "Potions cannot be used here"),
        NO_HEALING("No Healing", "Nothing heals you here but food"),
        STATUES("Statues", "They move only when no one is looking"),
        NARROWING_DOOR("The Narrowing Door", "Whoever leaves can't come back in. Someone must stay inside");

        private final String name;
        private final String desc;

        Rule(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public String key() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Rule byKey(String key) {
            for (Rule rule : values()) {
                if (rule.key().equals(key)) {
                    return rule;
                }
            }
            return null;
        }

        Component title() {
            return Component.translatableWithFallback("gate.mdvlcraft.rule." + this.key(), this.name);
        }

        Component description() {
            return Component.translatableWithFallback("gate.mdvlcraft.rule." + this.key() + ".desc", this.desc);
        }
    }

    static final double CHANCE = 0.06;
    static final double REWARD_FACTOR = 1.5;
    private static final String TABLET_TAG = "mdvlcraft_gate_tablet";
    private static final Map<Integer, Rule> RULES = new HashMap<>();
    private static final Set<Integer> ROLLED = new HashSet<>();
    /** Instances whose roll is forced to a rule (debug command). */
    static final Map<Integer, Rule> FORCED = new HashMap<>();
    /** Narrowing Door: who left each instance and may not come back in. */
    private static final Map<Integer, Set<UUID>> SHUT_OUT = new HashMap<>();
    /** Narrowing Door: roster members seen inside each instance (a member counts as leaving only once seen inside). */
    private static final Map<Integer, Set<UUID>> SEEN = new HashMap<>();
    /** Statues: the rooms of each instance that already have theirs. */
    private static final Map<Integer, Set<Integer>> STATUED = new HashMap<>();
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    @Nullable
    static Item trophy;
    @Nullable
    public static EntityType<GateStatue> statue;

    private RuleGates() {
    }

    @Nullable
    public static Rule rule(int instanceId) {
        return RULES.get(instanceId);
    }

    static void carry(int from, int to) {
        Rule rule = RULES.remove(from);
        if (rule != null) {
            RULES.put(to, rule);
        }
    }

    /**
     * GateEntity.tryEnter, for the first player of an empty roster, once the red roll has passed the gate by: an S gate
     * (or one forced by command) may become a trial gate. Runs once per instance.
     */
    public static void roll(ServerPlayer player, Instance instance) {
        if (instance.red || !ROLLED.add(instance.id)) {
            return;
        }
        Rule rule = FORCED.remove(instance.id);
        if (rule == null && (instance.rank != GateRank.S || player.getRandom().nextDouble() >= CHANCE)) {
            return;
        }
        if (rule == null) {
            rule = Rule.values()[player.getRandom().nextInt(Rule.values().length)];
        }
        RULES.put(instance.id, rule);
        ServerLevel level = player.server.getLevel(instance.dimension);
        if (level != null) {
            placeTablet(level, instance, rule);
        }
    }

    /** Two chiseled stone bricks 2–3 blocks from the entry, on the floor, inside the entrance room; the rule floats above. */
    static void placeTablet(ServerLevel level, Instance instance, Rule rule) {
        BlockPos entry = instance.entry;
        BlockPos spot = null;
        search:
        for (int distance = 2; distance <= 3; distance++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = entry.relative(direction, distance);
                boolean inRoom = instance.rooms.isEmpty() || instance.rooms.get(0).isInside(pos.above());
                if (inRoom && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir()
                    && !level.getBlockState(pos.below()).isAir()) {
                    spot = pos;
                    break search;
                }
            }
        }
        if (spot == null) {
            spot = entry.above(2);   // nowhere to stand it: only the text, above the entry
        } else {
            level.setBlock(spot, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(spot.above(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
            spot = spot.above(2);
        }
        Component text = Component.empty().append(Component.translatableWithFallback("gate.mdvlcraft.rule.tablet", "Trial").withStyle(ChatFormatting.GOLD))
            .append("\n").append(rule.title().copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append("\n").append(rule.description().copy().withStyle(ChatFormatting.GRAY));
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", Component.Serializer.toJson(text));
        tag.putString("billboard", "center");
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(spot.getX() + 0.5));
        pos.add(DoubleTag.valueOf(spot.getY() + 0.2));
        pos.add(DoubleTag.valueOf(spot.getZ() + 0.5));
        tag.put("Pos", pos);
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> e);
        if (display != null) {
            display.addTag(TABLET_TAG);
            display.getPersistentData().putInt(GateSpawner.TAG, instance.id);
            level.addFreshEntity(display);
        }
    }

    /** The rule as a title on every entry. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !GateDimensions.isGate(event.getTo())) {
            return;
        }
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        Rule rule = instance == null ? null : rule(instance.id);
        if (rule != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(rule.description()));
            player.connection.send(new ClientboundSetTitleTextPacket(rule.title().copy().withStyle(ChatFormatting.RED)));
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.entered", "Trial gate: %s. %s",
                rule.title(), rule.description()).withStyle(ChatFormatting.GOLD));
            player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.MASTER, 1.0F, 0.6F);
        }
    }

    /** The trial rule of the gate this player is on and inside, if any. */
    @Nullable
    static Rule ruleFor(Player player) {
        Instance instance = GateModifiers.playerInstance(player);
        return instance == null ? null : rule(instance.id);
    }

    /** No Potions: using one (drinking or throwing) is refused. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() instanceof PotionItem && ruleFor(event.getEntity()) == Rule.NO_POTIONS) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.getEntity().displayClientMessage(Rule.NO_POTIONS.description(), true);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getItem().getItem() instanceof PotionItem && event.getEntity() instanceof Player player && ruleFor(player) == Rule.NO_POTIONS) {
            event.setCanceled(true);
        }
    }

    /** No Healing: everything but food regeneration is cancelled. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player && ruleFor(player) == Rule.NO_HEALING && !fromFood()) {
            event.setCanceled(true);
        }
    }

    /** True if the heal being handled comes from natural regeneration (FoodData.tick). */
    static boolean fromFood() {
        return WALKER.walk(frames -> frames.anyMatch(frame -> frame.getDeclaringClass() == FoodData.class));
    }

    /** GateRewards.onCleared, per player: true if the boss chest rolls once more for them (trial gates, half the time). */
    public static boolean extraChestRoll(Instance instance, RandomSource random) {
        return rule(instance.id) != null && random.nextDouble() < REWARD_FACTOR - 1.0;
    }

    /** GateEntity.tryEnter: true (and the player is told) if the Narrowing Door has shut them out of this gate. */
    public static boolean shutOut(ServerPlayer player, Instance instance) {
        Set<UUID> out = SHUT_OUT.get(instance.id);
        if (out == null || !out.contains(player.getUUID())) {
            return false;
        }
        player.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.narrowing_door.shut",
            "The door has narrowed: you can't go back in").withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** Every 10 ticks, ACTIVE trial gates with a Statues or Narrowing Door rule. */
    private static void tickRules(MinecraftServer server, GateInstances gates) {
        for (Instance instance : List.copyOf(gates.all())) {
            Rule rule = rule(instance.id);
            ServerLevel level = server.getLevel(instance.dimension);
            if (level == null || instance.state != GateInstances.State.ACTIVE) {
                continue;
            }
            if (rule == Rule.NARROWING_DOOR) {
                narrow(server, level, gates, instance);
            } else if (rule == Rule.STATUES) {
                placeStatues(level, instance);
            }
        }
    }

    private static boolean inside(ServerPlayer player, Instance instance) {
        return player.level().dimension() == instance.dimension && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(player.position());
    }

    /** Narrowing Door: members seen inside who are now online, alive and out of the cell have left (by any way, even a
     * leave that already took them off the roster): off the roster and shut out; if nobody of the roster is inside, it fails. */
    private static void narrow(MinecraftServer server, ServerLevel level, GateInstances gates, Instance instance) {
        Set<UUID> seen = SEEN.computeIfAbsent(instance.id, k -> new HashSet<>());
        List<ServerPlayer> left = new ArrayList<>();
        for (UUID id : List.copyOf(seen)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive() && !inside(player, instance)) {   // offline keeps the place; the dead count once respawned
                left.add(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (instance.roster.contains(player.getUUID()) && inside(player, instance)) {
                seen.add(player.getUUID());
            }
        }
        if (left.isEmpty()) {
            return;
        }
        Component names = Component.empty();
        for (ServerPlayer player : left) {
            seen.remove(player.getUUID());
            instance.roster.remove(player.getUUID());
            SHUT_OUT.computeIfAbsent(instance.id, k -> new HashSet<>()).add(player.getUUID());
            names = names.copy().append(names.getString().isEmpty() ? Component.empty() : Component.literal(", ")).append(player.getDisplayName());
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.narrowing_door.left",
                "The door narrows behind you. You can't go back in.").withStyle(ChatFormatting.RED));
        }
        gates.setDirty();
        for (UUID id : instance.roster) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.narrowing_door.narrows",
                    "The door narrows: %s can't come back.", names).withStyle(ChatFormatting.GOLD));
            }
        }
        if (level.players().stream().noneMatch(p -> instance.roster.contains(p.getUUID()) && p.isAlive() && inside(p, instance))) {
            for (ServerPlayer player : left) {
                player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.narrowing_door.failed",
                    "Nobody stayed behind: the door shuts and the gate fails.").withStyle(ChatFormatting.DARK_RED));
            }
            GateCombat.fail(level, gates, instance);
        }
    }

    /** Statues: one in each triggered room but the entrance, on the floor as far from the players as the room allows. */
    private static void placeStatues(ServerLevel level, Instance instance) {
        if (statue == null) {
            return;
        }
        Set<Integer> done = STATUED.computeIfAbsent(instance.id, k -> new HashSet<>());
        for (int room : instance.triggeredRooms) {
            if (room == 0 || room >= instance.rooms.size() || !done.add(room)) {
                continue;
            }
            BoundingBox box = instance.rooms.get(room);
            BlockPos spot = statueSpot(level, instance, box);
            GateStatue mob = spot == null ? null : statue.create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.restrictTo(box.getCenter(), Math.max(box.getXSpan(), box.getZSpan()) / 2 + 1);
            mob.getPersistentData().putInt(GateStatue.INSTANCE, instance.id);
            GateSpawner.scale(mob, instance.rank);
            level.addFreshEntity(mob);
        }
    }

    /** A floor spot in the room (air for two blocks, something solid below), the farthest from any player of a few tries. */
    @Nullable
    private static BlockPos statueSpot(ServerLevel level, Instance instance, BoundingBox box) {
        BlockPos best = null;
        double bestDistance = -1.0;
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = attempt == 0 ? box.getCenter().getX() : Mth.randomBetweenInclusive(level.random, box.minX() + 2, box.maxX() - 2);
            int z = attempt == 0 ? box.getCenter().getZ() : Mth.randomBetweenInclusive(level.random, box.minZ() + 2, box.maxZ() - 2);
            for (int y = box.maxY() - 2; y > box.minY(); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.below()).isSolid()) {
                    double distance = level.players().stream().filter(p -> instance.roster.contains(p.getUUID()))
                        .mapToDouble(p -> p.distanceToSqr(Vec3.atCenterOf(pos))).min().orElse(Double.MAX_VALUE);
                    if (distance > bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
                    break;
                }
            }
        }
        return best;
    }

    /** GateRewards.onCleared: every player present at a trial gate's clear gets a Trial Trophy. */
    public static void onCleared(Instance instance, List<ServerPlayer> present) {
        Rule rule = rule(instance.id);
        if (rule == null || trophy == null) {
            return;
        }
        for (ServerPlayer player : present) {
            ItemStack stack = new ItemStack(trophy);
            stack.getOrCreateTag().putString("rule", rule.key());
            stack.getOrCreateTag().putString("rank", instance.rank.name());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.rule.cleared", "Trial passed: %s. You earned a Trial Trophy.",
                rule.title()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Tablets of closed gates go (a cell is reused by later gates). */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % 10 == 0) {
            tickRules(event.getServer(), GateInstances.get(event.getServer()));
        }
        if (event.getServer().getTickCount() % 100 != 0) {
            return;
        }
        GateInstances gates = GateInstances.get(event.getServer());
        RULES.keySet().removeIf(id -> gates.byId(id) == null);
        SHUT_OUT.keySet().removeIf(id -> gates.byId(id) == null);
        SEEN.keySet().removeIf(id -> gates.byId(id) == null);
        STATUED.keySet().removeIf(id -> gates.byId(id) == null);
        ServerLevel realm = event.getServer().getLevel(GateDimensions.GATE_REALM);
        if (realm != null) {
            realm.getEntities(EntityType.TEXT_DISPLAY, e -> stale(gates, e)).forEach(Entity::discard);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity() instanceof Display.TextDisplay && event.getLevel() instanceof ServerLevel level
            && stale(GateInstances.get(level.getServer()), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean stale(GateInstances gates, Entity entity) {
        if (!entity.getTags().contains(TABLET_TAG)) {
            return false;
        }
        Instance instance = gates.byId(GateSpawner.instanceOf(entity));
        return instance == null || !instance.live();
    }

    /** Registers the Trial Trophy item and the statue entity (no DeferredRegister, so MDVLBinder needs no line for them). */
    @EventBusSubscriber(modid = "mdvlcraft", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.ITEMS, helper -> {
                trophy = new TrophyItem();
                helper.register(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gate_trophy"), trophy);
            });
            event.register(ForgeRegistries.Keys.ENTITY_TYPES, helper -> {
                statue = EntityType.Builder.<GateStatue>of(GateStatue::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build("mdvlcraft:gate_statue");
                helper.register(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gate_statue"), statue);
            });
        }

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            if (statue != null) {
                event.put(statue, GateStatue.attributes().build());
            }
        }

        @SubscribeEvent
        public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES && trophy != null) {
                event.accept(trophy);
            }
        }
    }

    /** A keepsake from a cleared trial gate; NBT "rule" and "rank" say which. */
    static final class TrophyItem extends Item {
        TrophyItem() {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            Rule rule = stack.getTag() == null ? null : Rule.byKey(stack.getTag().getString("rule"));
            if (rule != null) {
                tooltip.add(Component.translatableWithFallback("gate.mdvlcraft.trophy.desc", "%s-rank trial: %s", stack.getTag().getString("rank"),
                    rule.title()).withStyle(ChatFormatting.GOLD));
            }
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }
}
