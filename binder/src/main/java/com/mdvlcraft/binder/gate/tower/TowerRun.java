package com.mdvlcraft.binder.gate.tower;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.gate.GateDimensions;
import com.mdvlcraft.binder.gate.GateInstances;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import com.mdvlcraft.binder.gate.GateMobs;
import com.mdvlcraft.binder.gate.GateModifiers;
import com.mdvlcraft.binder.gate.GateRank;
import com.mdvlcraft.binder.gate.GateRewardChestBlockEntity;
import com.mdvlcraft.binder.gate.GateRewards;
import com.mdvlcraft.binder.gate.GateSpawner;
import com.mdvlcraft.binder.gate.GateTheme;
import com.mdvlcraft.binder.gate.GateTravel;
import com.mdvlcraft.binder.gate.HiddenBoss;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.SkillsAPI;
import org.slf4j.Logger;

/**
 * A Demon Castle run (plan 10): a gate instance in the demon_castle dimension (so GateProtection, rosters, cells and
 * GateSpawner work as for gates) plus the tower's own state, kept here and not in the instance: the floor the party is
 * on, its layout and the next one's. Only those two floors exist; on reaching the top of the stairs the party moves up,
 * the floor below is cleared and the one above the new floor is built in its place (TowerFloor, one piece per tick from
 * a server-wide queue). The instance stays BUILDING with no rooms or markers of its own, so GateCombat's room triggers,
 * clear and collapse leave it alone; this class triggers rooms, spawns floor mobs and bosses (GateMobs, GateSpawner), and
 * gives the rewards. Runs aren't saved: every instance closes on restart.
 *
 * Progress (TowerProgress): standing on a landing raises a player's reached floor; beating a boss on floor 5k sets their
 * checkpoint to 5k + 1; dying sends them home and drops reached back to the checkpoint (the rest of the party carries
 * on); the lever on a landing or in a stair room sends them home keeping their floor; logging back in mid-run sends them
 * home. A run nobody is inside for EMPTY_TICKS closes.
 *
 * Each floor carries its tower modifiers (TowerModifiers, handed to GateModifiers for the instance), listed on a text
 * tablet over the landing. From floor 55 a boss floor has a HIDDEN_CHANCE chance that a hidden boss rises when its boss
 * falls (another theme's S boss, x1.5 health, glowing; it drops black essence stones through HiddenBoss.BLACK_TAG); the stairs
 * open when it dies. Mobs that other code adds to a run (Swarming's copies) get the floor tag and scaling on joining.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TowerRun {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int PARTY_CAP = 6;
    /** Persistent NBT int on tower mobs: their floor. */
    public static final String FLOOR_TAG = "mdvlcraft_tower_floor";
    /** Persistent NBT boolean (and scoreboard tag) on a floor's boss. */
    public static final String BOSS_TAG = "mdvlcraft_tower_boss";
    /** Persistent NBT float on mobs whose floor health would pass the cap: incoming damage is divided by it. */
    private static final String TOUGHNESS = "mdvlcraft_tower_toughness";
    private static final double HEALTH_CAP = 1024.0;
    private static final UUID FLOOR_HEALTH = UUID.fromString("0b7c6c1e-3d2a-4f0e-9b1d-5a7e2c9f0001");
    private static final UUID FLOOR_DAMAGE = UUID.fromString("0b7c6c1e-3d2a-4f0e-9b1d-5a7e2c9f0002");
    private static final int CHECK_TICKS = 10;
    private static final long EMPTY_TICKS = 2 * 60 * 20;
    private static final double PARTY_BONUS = 0.25;
    private static final int ALLY_RANGE = 32;
    private static final ResourceLocation FLOOR_LOOT = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gates/tower/floor");
    private static final ResourceLocation BOSS_LOOT = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "gates/tower/boss");
    /** Scoreboard tag on a hidden boss (for selectors; HiddenBoss.TAG is the persistent flag). */
    public static final String HIDDEN_TAG = "mdvlcraft_tower_hidden";
    private static final float HIDDEN_CHANCE = 0.25F;
    private static final double HIDDEN_HEALTH = 1.5;
    private static final UUID HIDDEN_HEALTH_ID = UUID.fromString("0b7c6c1e-3d2a-4f0e-9b1d-5a7e2c9f0003");
    /** Same tag as RuleGates' tablets, so a tablet loaded after a restart (its run closed) is dropped there. */
    private static final String TABLET_TAG = "mdvlcraft_gate_tablet";

    private static final Map<Integer, TowerRun> RUNS = new LinkedHashMap<>();
    /** Build and clear steps of every run, in order (a floor's clear always runs before the build that reuses its band). */
    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();

    public final int id;
    private int floor;
    private TowerFloor.Layout current;
    private TowerFloor.Layout next;
    private boolean currentReady;
    private boolean nextReady;
    private final Set<Integer> triggered = new HashSet<>();
    private boolean floorDone;
    private long emptySince = -1;
    private List<String> modifiers = List.of();
    private boolean hiddenRisen;
    /** Debug: the next boss death on a floor of 55+ raises the hidden boss for sure. */
    boolean forceHidden;
    /** True while this class spawns, so onJoin leaves its own mobs to made(). */
    private static boolean spawning;
    /** Players to send in once the first floor is built (the debug command). */
    private final List<UUID> sendIn = new ArrayList<>();

    private TowerRun(int id, int floor) {
        this.id = id;
        this.floor = floor;
    }

    @Nullable
    public static TowerRun get(int id) {
        return RUNS.get(id);
    }

    public static List<TowerRun> all() {
        return List.copyOf(RUNS.values());
    }

    public int floor() {
        return this.floor;
    }

    public boolean ready() {
        return this.currentReady;
    }

    public boolean nextReady() {
        return this.nextReady;
    }

    @Nullable
    public Instance instance(MinecraftServer server) {
        return GateInstances.get(server).byId(this.id);
    }

    public boolean live(MinecraftServer server) {
        Instance instance = this.instance(server);
        return instance != null && instance.live();
    }

    /** The live run the player opened with their sigil, if any. */
    @Nullable
    public static TowerRun ownedBy(MinecraftServer server, UUID player) {
        for (TowerRun run : RUNS.values()) {
            Instance instance = run.instance(server);
            if (instance != null && instance.live() && player.equals(instance.owner)) {
                return run;
            }
        }
        return null;
    }

    /** The run whose roster holds this player, if any. */
    @Nullable
    public static TowerRun of(ServerPlayer player) {
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        return instance == null || instance.dimension != GateDimensions.DEMON_CASTLE ? null : RUNS.get(instance.id);
    }

    /**
     * A new run for `opener` at the floor they chose in the sigil menu (CastleSigilItem.allowed), or lower if an ally
     * standing within ALLY_RANGE has reached less, so nobody skips floors. Its first two floors are queued for building.
     * Null if the castle world is missing.
     */
    @Nullable
    public static TowerRun open(ServerPlayer opener, int floor) {
        int start = floor;
        for (ServerPlayer other : opener.serverLevel().players()) {
            if (other != opener && other.distanceTo(opener) <= ALLY_RANGE && AllianceAPI.areAllied(opener, other)) {
                start = Math.min(start, TowerProgress.get(other.server).of(other.getUUID()).reached);
            }
        }
        return open(opener.server, opener.getUUID(), start);
    }

    /** A run starting at `floor`, owned by `owner` (null: anyone may join, for debug runs). */
    @Nullable
    public static TowerRun open(MinecraftServer server, @Nullable UUID owner, int floor) {
        ServerLevel level = server.getLevel(GateDimensions.DEMON_CASTLE);
        if (level == null) {
            return null;
        }
        GateInstances gates = GateInstances.get(server);
        Instance instance = gates.create(GateDimensions.DEMON_CASTLE, TowerFloor.theme(floor), GateRank.A, level.getGameTime());
        instance.owner = owner;
        TowerRun run = new TowerRun(instance.id, floor);
        RUNS.put(run.id, run);
        run.current = TowerFloor.plan(level.getSeed(), floor, instance.cellX, instance.cellZ);
        run.next = TowerFloor.plan(level.getSeed(), floor + 1, instance.cellX, instance.cellZ);
        instance.entry = run.current.landing();
        run.applyModifiers(level);
        gates.setDirty();
        TowerFloor.Layout first = run.current;
        QUEUE.addAll(TowerFloor.buildSteps(level, first));
        QUEUE.add(() -> {
            if (run.current == first) {
                run.currentReady = true;
                run.placeCrystals(level);
                run.placeTablet(level);
                run.sendIn.forEach(uuid -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        run.enter(player, null);
                    }
                });
                run.sendIn.clear();
            }
        });
        run.queueNext(level);
        return run;
    }

    /** Debug: send this player in as soon as the first floor stands (now, if it does). */
    public void sendInWhenReady(ServerPlayer player) {
        if (this.currentReady) {
            this.enter(player, null);
        } else {
            this.sendIn.add(player.getUUID());
        }
    }

    private void queueNext(ServerLevel level) {
        TowerFloor.Layout layout = this.next;
        this.nextReady = false;
        QUEUE.addAll(TowerFloor.buildSteps(level, layout));
        QUEUE.add(() -> {
            if (this.next == layout) {
                this.nextReady = true;
            }
        });
    }

    /** Roster members inside the run's cell, alive and playing. */
    List<ServerPlayer> present(MinecraftServer server) {
        Instance instance = this.instance(server);
        List<ServerPlayer> present = new ArrayList<>();
        ServerLevel level = server.getLevel(GateDimensions.DEMON_CASTLE);
        if (instance == null || level == null) {
            return present;
        }
        for (ServerPlayer player : level.players()) {
            if (instance.roster.contains(player.getUUID()) && player.isAlive() && !player.isSpectator()
                && GateDimensions.bounds(instance.cellX, instance.cellZ).contains(player.position())) {
                present.add(player);
            }
        }
        return present;
    }

    int inside(MinecraftServer server) {
        return this.present(server).size();
    }

    /**
     * A player walked into the castle gate: on the roster already, or the opener / an ally of the online opener while
     * the roster is under PARTY_CAP. A newcomer must have reached the run's floor themselves (no skipping floors).
     */
    void tryEnter(ServerPlayer player, Vec3 returnAt) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        GateInstances gates = GateInstances.get(player.server);
        Instance instance = this.instance(player.server);
        if (instance == null) {
            return;
        }
        Instance theirs = gates.ofPlayer(player.getUUID());
        if (theirs != null && theirs != instance) {
            player.displayClientMessage(Component.translatable("gate.mdvlcraft.other_gate"), true);
            return;
        }
        if (!instance.roster.contains(player.getUUID())) {
            if (!this.mayJoin(player, instance)) {
                player.displayClientMessage(Component.translatable("gate.mdvlcraft.not_party"), true);
                return;
            }
            if (instance.roster.size() >= PARTY_CAP) {
                player.displayClientMessage(Component.translatable("gate.mdvlcraft.full", PARTY_CAP), true);
                return;
            }
            int reached = TowerProgress.get(player.server).of(player.getUUID()).reached;
            if (reached < this.floor) {
                player.displayClientMessage(Component.translatableWithFallback("tower.mdvlcraft.not_reached",
                    "You have only reached floor %s; the party is on floor %s.", reached, this.floor), true);
                return;
            }
        }
        this.enter(player, returnAt);
    }

    private boolean mayJoin(ServerPlayer player, Instance instance) {
        if (instance.owner == null || instance.owner.equals(player.getUUID())) {
            return true;
        }
        ServerPlayer owner = player.server.getPlayerList().getPlayer(instance.owner);
        return owner != null && AllianceAPI.areAllied(owner, player);
    }

    /** Into the run at the current floor's landing; the floor counts as reached. */
    void enter(ServerPlayer player, @Nullable Vec3 returnAt) {
        Instance instance = this.instance(player.server);
        if (instance != null && GateTravel.enter(player, instance, returnAt)) {
            TowerProgress.get(player.server).reach(player.getUUID(), this.floor);
            this.announceFloor(player);
        }
    }

    private void announceFloor(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("tower.mdvlcraft.floor", "Floor %s", this.floor)
            .withStyle(ChatFormatting.DARK_RED)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(TowerFloor.isBoss(this.floor)
            ? Component.translatableWithFallback("tower.mdvlcraft.floor.boss", "A boss awaits").withStyle(ChatFormatting.GOLD)
            : Component.translatable("gate.mdvlcraft.theme." + TowerFloor.theme(this.floor).key()).withStyle(ChatFormatting.GRAY)));
        Component names = TowerModifiers.names(this.modifiers);
        if (names != null) {
            player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.modifiers", "Tower modifiers: %s", names).withStyle(ChatFormatting.GOLD));
        }
    }

    /** The current floor's tower modifiers go to the instance (GateModifiers applies them as in a gate). */
    private void applyModifiers(ServerLevel level) {
        this.modifiers = TowerModifiers.of(level.getSeed(), this.floor);
        GateModifiers.attach(this.id, this.modifiers);
    }

    public List<String> modifiers() {
        return this.modifiers;
    }

    /** A floating text over the landing: the floor and its modifiers. It carries the floor tag, so it goes with the floor's mobs. */
    private void placeTablet(ServerLevel level) {
        Component names = TowerModifiers.names(this.modifiers);
        Component text = Component.empty().append(Component.translatableWithFallback("tower.mdvlcraft.floor", "Floor %s", this.floor)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .append("\n").append(names != null ? names
                : Component.translatableWithFallback("tower.mdvlcraft.no_modifiers", "No tower modifiers").withStyle(ChatFormatting.GRAY));
        BlockPos at = this.current.landing().offset(-2, 2, 0);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", Component.Serializer.toJson(text));
        tag.putString("billboard", "center");
        tag.putString("alignment", "center");   // read unconditionally in 1.20.1: without it the load logs "Not a string"
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(at.getX() + 0.5));
        pos.add(DoubleTag.valueOf(at.getY() + 0.2));
        pos.add(DoubleTag.valueOf(at.getZ() + 0.5));
        tag.put("Pos", pos);
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> e);
        if (display != null) {
            display.addTag(TABLET_TAG);
            display.getPersistentData().putInt(GateSpawner.TAG, this.id);
            display.getPersistentData().putInt(FLOOR_TAG, this.floor);
            level.addFreshEntity(display);
        }
    }

    /** The current floor's crystal veins (GateRewards places them at the instance's crystal markers). */
    private void placeCrystals(ServerLevel level) {
        Instance instance = this.instance(level.getServer());
        if (instance == null) {
            return;
        }
        instance.markers.clear();
        this.current.crystals().forEach(pos -> instance.markers.add(new Marker("crystal", pos, -1)));
        GateRewards.placeCrystals(level, instance);
    }

    // ---- ticking ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Runnable step = QUEUE.poll();
        if (step != null) {
            try {
                step.run();
            } catch (RuntimeException e) {
                LOGGER.error("Demon Castle build step failed", e);
            }
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS != 0 || RUNS.isEmpty()) {
            return;
        }
        ServerLevel level = server.getLevel(GateDimensions.DEMON_CASTLE);
        for (TowerRun run : List.copyOf(RUNS.values())) {
            if (level == null || !run.live(server)) {
                run.end(server, false);
            } else {
                run.tick(level);
            }
        }
    }

    private void tick(ServerLevel level) {
        List<ServerPlayer> present = this.present(level.getServer());
        if (present.isEmpty()) {
            if (this.emptySince < 0) {
                this.emptySince = level.getGameTime();
            } else if (this.currentReady && level.getGameTime() - this.emptySince > EMPTY_TICKS) {
                this.end(level.getServer(), true);
            }
            return;
        }
        this.emptySince = -1;
        if (!this.currentReady) {
            return;
        }
        for (int room = 0; room < this.current.rooms().size(); room++) {
            if (this.triggered.contains(room)) {
                continue;
            }
            for (ServerPlayer player : present) {
                if (this.current.room(room).isInside(player.blockPosition())) {
                    this.triggered.add(room);
                    this.spawnRoom(level, room, present.size());
                    break;
                }
            }
        }
        if (!this.floorDone && !TowerFloor.isBoss(this.floor) && this.triggered.size() == this.current.rooms().size() && this.mobsLeft(level) == 0) {
            this.floorDone = true;
            this.reward(level, present, false);
        }
        for (ServerPlayer player : present) {
            if (this.current.stairTop().isInside(player.blockPosition())) {
                if (this.nextReady) {
                    this.advance(level, present);
                } else {
                    player.displayClientMessage(Component.translatableWithFallback("tower.mdvlcraft.building", "The next floor is still rising..."), true);
                }
                break;
            }
        }
    }

    private int mobsLeft(ServerLevel level) {
        int left = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob && entity.isAlive() && GateSpawner.instanceOf(entity) == this.id && entity.getPersistentData().getInt(FLOOR_TAG) == this.floor) {
                left++;
            }
        }
        return left;
    }

    /** Everyone on the floor goes up to the next landing; the floor below is cleared and the one above is built. */
    void advance(ServerLevel level, List<ServerPlayer> present) {
        Instance instance = this.instance(level.getServer());
        if (instance == null || !this.nextReady) {
            return;
        }
        TowerFloor.Layout old = this.current;
        int oldFloor = this.floor;
        this.floor++;
        this.current = this.next;
        this.currentReady = true;
        this.triggered.clear();
        this.floorDone = false;
        this.hiddenRisen = false;
        this.forceHidden = false;
        instance.entry = this.current.landing();
        GateInstances.get(level.getServer()).setDirty();
        this.applyModifiers(level);
        BlockPos landing = this.current.landing();
        for (ServerPlayer player : present) {
            player.teleportTo(level, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, player.getYRot(), 0.0F);
            player.fallDistance = 0.0F;
            TowerProgress.get(level.getServer()).reach(player.getUUID(), this.floor);
            this.announceFloor(player);
        }
        level.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.7F);
        this.placeCrystals(level);
        this.discardMobs(level, oldFloor);
        this.placeTablet(level);
        QUEUE.addAll(TowerFloor.clearSteps(level, old));
        this.next = TowerFloor.plan(level.getSeed(), this.floor + 1, instance.cellX, instance.cellZ);
        this.queueNext(level);
    }

    /** Ends the run: anyone still inside goes home (when `sendHome`), its mobs go, both floors are cleared, the cell is freed. */
    public void end(MinecraftServer server, boolean sendHome) {
        RUNS.remove(this.id);
        GateInstances gates = GateInstances.get(server);
        Instance instance = gates.byId(this.id);
        ServerLevel level = server.getLevel(GateDimensions.DEMON_CASTLE);
        if (instance != null && sendHome) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (instance.roster.contains(player.getUUID()) && player.level().dimension() == GateDimensions.DEMON_CASTLE) {
                    GateTravel.leave(player, instance, true);
                }
            }
        }
        if (level != null) {
            this.discardMobs(level, -1);
            QUEUE.addAll(TowerFloor.clearSteps(level, this.current));
            QUEUE.addAll(TowerFloor.clearSteps(level, this.next));
        }
        if (instance != null && instance.live()) {
            gates.close(instance);
        }
    }

    /** Removes the run's mobs on `floor` (-1: all of them). */
    private void discardMobs(ServerLevel level, int floor) {
        List<Entity> gone = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (GateSpawner.instanceOf(entity) == this.id && (floor < 0 || entity.getPersistentData().getInt(FLOOR_TAG) == floor)) {
                gone.add(entity);
            }
        }
        gone.forEach(Entity::discard);
    }

    // ---- mobs ----

    /** A room's mobs: spawn points × the A-rank count factor × the party bonus, plus its elite; the arena gets the boss. */
    private void spawnRoom(ServerLevel level, int room, int partySize) {
        Instance instance = this.instance(level.getServer());
        if (instance == null) {
            return;
        }
        if (TowerFloor.isBoss(this.floor)) {
            this.spawnBoss(level, instance);
            return;
        }
        List<TowerFloor.Spawn> normal = new ArrayList<>();
        for (TowerFloor.Spawn spawn : this.current.spawns().get(room)) {
            if (spawn.elite()) {
                this.spawnOne(level, instance, GateMobs.Kind.ELITE, spawn.pos(), GateSpawner.ELITE);
            } else {
                normal.add(spawn);
            }
        }
        if (normal.isEmpty()) {
            return;
        }
        double party = 1.0 + PARTY_BONUS * Math.max(0, partySize - 1);
        int count = (int)Math.round(normal.size() * GateMobs.countFactor(instance.rank) * party);
        for (int i = 0; i < count; i++) {
            TowerFloor.Spawn spawn = normal.get(i % normal.size());
            this.spawnOne(level, instance, spawn.ranged() ? GateMobs.Kind.RANGED : GateMobs.Kind.MELEE, spawn.pos(), null);
        }
    }

    @Nullable
    private Mob spawnOne(ServerLevel level, Instance instance, GateMobs.Kind kind, BlockPos pos, @Nullable String role) {
        EntityType<?> type = GateMobs.pick(TowerFloor.theme(this.floor), instance.rank, kind, level.random);
        return type == null ? null : this.made(spawn(level, instance, type, pos, role));
    }

    /** GateSpawner.spawn with the onJoin scaling off (made() does it). */
    @Nullable
    private static Mob spawn(ServerLevel level, Instance instance, EntityType<?> type, BlockPos pos, @Nullable String role) {
        spawning = true;
        try {
            return GateSpawner.spawn(level, instance, type, pos, role, -1);
        } finally {
            spawning = false;
        }
    }

    /** Mobs added to a run by other code (Swarming's copies) get the current floor's tag and scaling. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (spawning || event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob) || mob.getPersistentData().contains(FLOOR_TAG)
            || event.getLevel().dimension() != GateDimensions.DEMON_CASTLE) {
            return;
        }
        TowerRun run = RUNS.get(GateSpawner.instanceOf(mob));
        if (run != null) {
            run.made(mob);
        }
    }

    /** Boss rank by floor (plan 10.2): floors 5–20 the B bosses ("Champion"), 25–50 A ("Legend"), 55 on S. */
    static GateRank bossRank(int floor) {
        return floor <= 20 ? GateRank.B : floor <= 50 ? GateRank.A : GateRank.S;
    }

    private void spawnBoss(ServerLevel level, Instance instance) {
        GateMobs.Boss boss = GateMobs.boss(TowerFloor.theme(this.floor), bossRank(this.floor));
        EntityType<?> type = GateMobs.type(boss.id());
        if (type == null) {
            LOGGER.warn("Tower boss {} is not registered; using an elite instead", boss.id());
            type = GateMobs.pick(TowerFloor.theme(this.floor), instance.rank, GateMobs.Kind.ELITE, level.random);
        }
        for (int i = 0; type != null && i < boss.count(); i++) {
            BlockPos pos = this.current.boss().offset(i * 3 - (boss.count() - 1), 0, 0);
            Mob mob = this.made(spawn(level, instance, type, pos, GateSpawner.BOSS));
            if (mob != null) {
                mob.getPersistentData().putBoolean(BOSS_TAG, true);
                mob.addTag(BOSS_TAG);
            }
        }
    }

    /**
     * The boss is dead on a floor of 55+: with HIDDEN_CHANCE (or when forced) a hidden boss rises in the arena and the bars
     * stay shut (true). Another theme's S boss if one is loaded, else this theme's, else an elite.
     */
    private boolean riseHidden(ServerLevel level) {
        Instance instance = this.instance(level.getServer());
        if (instance == null) {
            return false;
        }
        GateTheme theme = TowerFloor.theme(this.floor);
        List<GateTheme> others = new ArrayList<>(List.of(GateTheme.values()));
        others.remove(theme);
        EntityType<?> type = null;
        while (type == null && !others.isEmpty()) {
            type = GateMobs.type(GateMobs.boss(others.remove(level.random.nextInt(others.size())), GateRank.S).id());
        }
        if (type == null) {
            type = GateMobs.type(GateMobs.boss(theme, GateRank.S).id());
        }
        if (type == null) {
            type = GateMobs.pick(theme, instance.rank, GateMobs.Kind.ELITE, level.random);
        }
        Mob boss = type == null ? null : spawn(level, instance, type, this.current.boss(), GateSpawner.BOSS);
        if (boss == null) {
            return false;
        }
        modify(boss.getAttribute(Attributes.MAX_HEALTH), HIDDEN_HEALTH_ID, HIDDEN_HEALTH - 1.0);   // before the floor scaling, which reads max health
        this.made(boss);
        boss.getPersistentData().putBoolean(BOSS_TAG, true);
        boss.getPersistentData().putBoolean(HiddenBoss.TAG, true);
        boss.getPersistentData().putBoolean(HiddenBoss.BLACK_TAG, true);   // black essence stones (GateRewards.onDrops)
        boss.addTag(BOSS_TAG);
        boss.addTag(HIDDEN_TAG);
        boss.setGlowingTag(true);
        boss.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.hidden.name", "Hidden %s", type.getDescription()).withStyle(ChatFormatting.DARK_RED));
        for (ServerPlayer player : this.present(level.getServer())) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatableWithFallback("tower.mdvlcraft.hidden.subtitle",
                "The stairs stay barred")));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.hidden.title",
                "Something else stirs").withStyle(ChatFormatting.DARK_RED)));
            player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.hidden.message",
                "A hidden boss rises in the arena. The stairs open when it falls.").withStyle(ChatFormatting.RED));
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        return true;
    }

    /** Tags a fresh tower mob with its floor and adds the floor scaling: +5% health and +4% damage per floor, compounding.
     * Health stops at HEALTH_CAP; the rest becomes damage reduction (TOUGHNESS). */
    @Nullable
    private Mob made(@Nullable Mob mob) {
        if (mob == null) {
            return null;
        }
        mob.getPersistentData().putInt(FLOOR_TAG, this.floor);
        double health = Math.pow(1.05, this.floor - 1);
        double damage = Math.pow(1.04, this.floor - 1);
        double wanted = mob.getMaxHealth() * health;
        if (wanted > HEALTH_CAP && mob.getMaxHealth() < HEALTH_CAP) {
            mob.getPersistentData().putFloat(TOUGHNESS, (float)(wanted / HEALTH_CAP));
            health = HEALTH_CAP / mob.getMaxHealth();
        }
        modify(mob.getAttribute(Attributes.MAX_HEALTH), FLOOR_HEALTH, health - 1.0);
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE), FLOOR_DAMAGE, damage - 1.0);
        mob.setHealth(mob.getMaxHealth());
        return mob;
    }

    private static void modify(@Nullable AttributeInstance attribute, UUID id, double amount) {
        if (attribute != null && amount > 0.0 && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, "tower floor", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        float toughness = event.getEntity().getPersistentData().getFloat(TOUGHNESS);
        if (toughness > 1.0F) {
            event.setAmount(event.getAmount() / toughness);
        }
    }

    // ---- rewards ----

    /**
     * Essence stone factor for a castle mob's drops (GateRewards.onDrops): 0.5 on a farm floor, more than 10 below the best
     * floor of the player who gets the kill (or of the owner of the summon that does), else 1. Not a castle mob: 1.
     */
    public static double dropFactor(LivingEntity mob) {
        if (mob.level().dimension() != GateDimensions.DEMON_CASTLE || !mob.getPersistentData().contains(FLOOR_TAG)) {
            return 1.0;
        }
        LivingEntity killer = mob.getKillCredit();
        if (killer instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner) {
            killer = owner;
        }
        return killer instanceof ServerPlayer player ? dropFactor(player, mob.getPersistentData().getInt(FLOOR_TAG)) : 1.0;
    }

    static double dropFactor(ServerPlayer player, int floor) {
        return floor < TowerProgress.get(player.server).of(player.getUUID()).best - 10 ? 0.5 : 1.0;
    }

    /**
     * The floor's chest (stair room on a normal floor, the arena centre on a boss floor) with one roll per present member.
     * Boss floors add Tower Seals (2, doubled on a player's first clear of that floor) and, on a first clear, 6000 × (1 +
     * floor / 50) skill experience. Floors more than 10 below a player's best give half (a 50% chance at the chest roll,
     * half the seals).
     */
    private void reward(ServerLevel level, List<ServerPlayer> present, boolean boss) {
        BlockPos pos = boss ? this.current.boss() : this.current.chest();
        level.setBlockAndUpdate(pos, GateRewards.REWARD_CHEST.get().defaultBlockState());
        GateRewardChestBlockEntity chest = level.getBlockEntity(pos) instanceof GateRewardChestBlockEntity c ? c : null;
        TowerProgress progress = TowerProgress.get(level.getServer());
        for (ServerPlayer player : present) {
            boolean farming = this.floor < progress.of(player.getUUID()).best - 10;
            boolean first = boss && progress.bossBeaten(player.getUUID(), this.floor);
            if (chest != null && (!farming || level.random.nextBoolean())) {
                chest.roll(player, boss ? BOSS_LOOT : FLOOR_LOOT);
            }
            if (boss) {
                int seals = (first ? 4 : 2) / (farming ? 2 : 1);
                player.getInventory().placeItemBackInInventory(new ItemStack(TowerItems.TOWER_SEAL.get(), seals));
                int experience = first ? (int)(6000 * (1.0 + this.floor / 50.0)) : 0;
                if (experience > 0) {
                    SkillsAPI.streamUnlockedCategories(player).forEach(category -> category.getExperience().ifPresent(e -> e.addTotal(player, experience)));
                }
                player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.boss_cleared",
                    "Floor %s's boss is beaten! +%s Tower Seals, +%s skill experience. The stairs are open.", this.floor, seals, experience)
                    .withStyle(ChatFormatting.GOLD));
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.floor_cleared",
                    "Floor %s is clear: a chest waits in the stair room.", this.floor).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // ---- events ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (event.isCanceled() || !(dead.level() instanceof ServerLevel level) || level.dimension() != GateDimensions.DEMON_CASTLE) {
            return;
        }
        if (dead instanceof ServerPlayer player) {
            TowerRun run = of(player);
            if (run != null) {
                TowerProgress progress = TowerProgress.get(player.server);
                progress.died(player.getUUID());
                Instance instance = run.instance(player.server);
                if (instance != null) {
                    instance.roster.remove(player.getUUID());
                    GateInstances.get(player.server).setDirty();
                }
                player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.died",
                    "You fell on floor %s. Next time you start from floor %s.", run.floor, progress.of(player.getUUID()).reached)
                    .withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (!dead.getPersistentData().getBoolean(BOSS_TAG)) {
            return;
        }
        dead.getPersistentData().remove(BOSS_TAG);   // a boss with a second phase counts once
        TowerRun run = RUNS.get(GateSpawner.instanceOf(dead));
        if (run == null || run.floorDone || dead.getPersistentData().getInt(FLOOR_TAG) != run.floor) {
            return;
        }
        for (Entity other : level.getAllEntities()) {
            if (other != dead && other.isAlive() && other.getPersistentData().getBoolean(BOSS_TAG) && GateSpawner.instanceOf(other) == run.id) {
                return;   // its twin still stands
            }
        }
        if (run.floor >= TowerModifiers.LEGEND_FLOOR && !run.hiddenRisen) {
            run.hiddenRisen = true;
            if ((run.forceHidden || level.random.nextFloat() < HIDDEN_CHANCE) && run.riseHidden(level)) {
                return;
            }
        }
        run.floorDone = true;
        TowerFloor.openBars(level, run.current);
        run.reward(level, run.present(level.getServer()), true);
    }

    /** The Leave Tower lever: home, keeping the floor reached. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != GateDimensions.DEMON_CASTLE
            || !player.level().getBlockState(event.getPos()).is(Blocks.LEVER)) {
            return;
        }
        TowerRun run = of(player);
        if (run == null) {
            return;
        }
        event.setCanceled(true);
        GateTravel.leave(player, run.instance(player.server), true);
        player.sendSystemMessage(Component.translatableWithFallback("tower.mdvlcraft.left", "You leave the Demon Castle. You will continue from floor %s.",
            TowerProgress.get(player.server).of(player.getUUID()).reached));
    }

    /** Logging in inside the castle (after a logout or crash mid-run): home; the floor reached is already saved. Also sends
     * the status page's tower line. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() == GateDimensions.DEMON_CASTLE && !player.isCreative()) {
            GateTravel.leave(player, GateInstances.get(player.server).ofPlayer(player.getUUID()), true);
        }
        TowerStatusPacket.send(player);   // the status page's tower line
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RUNS.clear();
        QUEUE.clear();
    }

    /** For the debug command: the run's state in one line. */
    public String describe(MinecraftServer server) {
        Instance instance = this.instance(server);
        return "Run " + this.id + ": floor " + this.floor + (TowerFloor.isBoss(this.floor) ? " (boss)" : "") + ", ready " + this.currentReady
            + ", next ready " + this.nextReady + ", rooms " + this.triggered.size() + "/" + this.current.rooms().size() + ", done " + this.floorDone
            + ", roster " + (instance == null ? 0 : instance.roster.size()) + ", inside " + this.inside(server)
            + (instance == null ? "" : ", cell " + instance.cellX + "," + instance.cellZ) + ", modifiers " + this.modifiers
            + (this.hiddenRisen ? ", hidden boss rolled" : this.forceHidden ? ", hidden boss forced" : "");
    }

    /** For the debug command: the landing of the current floor. */
    public BlockPos landing() {
        return this.current.landing();
    }

    public TowerFloor.Layout layout() {
        return this.current;
    }
}
