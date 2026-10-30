package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.GateInstances.Marker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The gate interior variety set's moving parts (rooms from tools/gates/gen_gate_variety.py, picked by the user):
 *   trap:flame      vents that burst into flame on a beat (two beats, alternating, so there is always a way through)
 *   bars            a portcullis that drops when its corridor's fight starts and lifts once its mobs are down
 *   lever           three levers to pull in the right order (a wrong pull calls mobs and resets them): a chest at `reward`
 *   ritual          step into the circle: three waves at the `wave` markers, then a chest
 *   gauntlet        step into the circle: survive 60 seconds of mobs from the `wave` markers, then a chest
 *   collapse        the deck crumbles patch by patch once the fight starts
 *   fountain        stand in front of it: full health, once
 *   altar           stand before it: the party gets one random 3-minute blessing, once
 *   cursed_shrine   stand before it: a chest now, and the next room's fight is harder
 *   merchant        a ghost merchant selling supplies for the gate's mana crystals
 *   spawn:champion  a champion (an elite with double health) guards its room's reward
 *   spawn:spider    a spider from the nest
 * and events: trapped loot chests (an ambush), a treasure goblin in some gates, ceiling ambushes, reinforcements when a
 * room's fight is half over, lights going out, and each gate mood's ambient particles and sounds (GatePalettes).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateFeatures {
    private static final MobEffect[] BLESSINGS = {MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_RESISTANCE,
        MobEffects.REGENERATION, MobEffects.DIG_SPEED, MobEffects.LUCK, MobEffects.FIRE_RESISTANCE};
    /** Per instance: the state of its features (in memory: a restart closes every gate). */
    private static final Map<Integer, State> STATES = new HashMap<>();
    /** Trapped loot chests by instance. */
    private static final Map<Integer, Set<BlockPos>> TRAPPED = new HashMap<>();

    private GateFeatures() {
    }

    private static final class State {
        final Set<BlockPos> used = new HashSet<>();               // fountains, altars and shrines already used
        final Map<Integer, List<Integer>> pulls = new HashMap<>(); // lever puzzle: pulls so far by room
        final Map<Integer, int[]> leverOrder = new HashMap<>();
        final Set<Integer> barsDown = new HashSet<>();
        final Map<Integer, Long> collapseFrom = new HashMap<>();
        final Map<Integer, Integer> collapsed = new HashMap<>();
        final Map<Integer, Integer> wave = new HashMap<>();         // ritual: waves done by room (-1 finished)
        final Map<Integer, Long> gauntletUntil = new HashMap<>();
        final Map<Integer, Integer> reinforce = new HashMap<>();    // rooms that get reinforcements: their first count
        final Map<Integer, Long> lightsOut = new HashMap<>();
        final Set<Integer> rewarded = new HashSet<>();
        boolean cursedNext;
        int goblinRoom = -1;
    }

    private static State state(Instance instance) {
        return STATES.computeIfAbsent(instance.id, id -> new State());
    }

    static void trap(Instance instance, BlockPos chest) {
        TRAPPED.computeIfAbsent(instance.id, id -> new HashSet<>()).add(chest.immutable());
    }

    // ---- built ----

    /** Once the gate is built: levers on their walls, the ghost merchant, and whether a treasure goblin waits. */
    static void onBuilt(ServerLevel level, Instance instance) {
        State state = state(instance);
        Random random = new Random(instance.id * 104729L);
        for (Marker marker : instance.markers) {
            switch (marker.kind()) {
                case "lever" -> {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (level.getBlockState(marker.pos().relative(d)).isSolidRender(level, marker.pos().relative(d))) {
                            level.setBlock(marker.pos(), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                                .setValue(LeverBlock.FACING, d.getOpposite()), Block.UPDATE_ALL);
                            break;
                        }
                    }
                }
                case "merchant" -> merchant(level, instance, marker.pos());
                default -> {
                }
            }
        }
        if (random.nextInt(100) < 25) {
            List<Integer> rooms = new ArrayList<>();
            for (Marker marker : instance.markers) {
                if (marker.kind().equals("spawn:melee") && !rooms.contains(marker.room())) {
                    rooms.add(marker.room());
                }
            }
            if (!rooms.isEmpty()) {
                state.goblinRoom = rooms.get(random.nextInt(rooms.size()));
            }
        }
    }

    private static void merchant(ServerLevel level, Instance instance, BlockPos pos) {
        WanderingTrader trader = EntityType.WANDERING_TRADER.create(level);
        if (trader == null) {
            return;
        }
        trader.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0F, 0.0F);
        trader.setNoAi(true);
        trader.setInvulnerable(true);
        trader.setPersistenceRequired();
        trader.setDespawnDelay(0);
        trader.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.ghost_merchant", "Ghost Merchant").withStyle(ChatFormatting.AQUA));
        trader.setCustomNameVisible(true);
        trader.setGlowingTag(true);
        trader.getPersistentData().putInt("mdvlcraft_gate_merchant", instance.id);
        ItemStack crystal = new ItemStack(GateItems.MANA_CRYSTALS.get(instance.rank).get());
        trader.getOffers().clear();
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(1), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING), 4, 0, 0.0F));
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(2), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LONG_REGENERATION), 3, 0, 0.0F));
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(2), new ItemStack(Items.GOLDEN_APPLE), 3, 0, 0.0F));
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(1), new ItemStack(Items.ARROW, 24), 4, 0, 0.0F));
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(3), PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_STRENGTH), 2, 0, 0.0F));
        trader.getOffers().add(new MerchantOffer(crystal.copyWithCount(8), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 0, 0.0F));
        level.addFreshEntity(trader);
    }

    // ---- a room's fight starts (GateCombat.spawnRoom) ----

    static void onRoomTriggered(ServerLevel level, Instance instance, int room, int partySize) {
        State state = state(instance);
        long now = level.getGameTime();
        Random random = new Random(instance.id * 31L + room);
        BoundingBox box = room >= 0 && room < instance.rooms.size() ? instance.rooms.get(room) : null;
        boolean combat = has(instance, room, "spawn:melee") || has(instance, room, "spawn:ranged");
        for (Marker marker : instance.markers) {
            if (marker.room() != room) {
                continue;
            }
            if (marker.kind().equals("bars") && level.getBlockState(marker.pos()).isAir()) {
                for (int y = 0; y < 4; y++) {
                    if (level.getBlockState(marker.pos().above(y)).isAir()) {
                        level.setBlock(marker.pos().above(y), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                state.barsDown.add(room);
                level.playSound(null, marker.pos(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
            }
        }
        if (has(instance, room, "collapse")) {
            state.collapseFrom.put(room, now + 100);
        }
        if (!combat || box == null || room == GateCombat.bossRoom(instance)) {
            return;
        }
        // the cursed shrine's price: this room fights harder
        if (state.cursedNext) {
            state.cursedNext = false;
            spawnAround(level, instance, room, GateMobs.Kind.ELITE, 1, 0);
            spawnAround(level, instance, room, GateMobs.Kind.MELEE, 2 + partySize, 0);
        }
        // ceiling ambush: some come down from above
        if (random.nextInt(100) < 20) {
            spawnAround(level, instance, room, GateMobs.Kind.MELEE, 2 + random.nextInt(2), 5);
        }
        // reinforcements arrive once the fight is half over
        if (random.nextInt(100) < 25) {
            state.reinforce.put(room, alive(level, instance, room));
        }
        // the lights go out
        if (random.nextInt(100) < 15) {
            state.lightsOut.put(room, now + 60);
        }
        if (room == state.goblinRoom) {
            goblin(level, instance, room);
        }
    }

    private static boolean has(Instance instance, int room, String kind) {
        return instance.markers.stream().anyMatch(m -> m.room() == room && m.kind().equals(kind));
    }

    private static List<Marker> markers(Instance instance, int room, String kind) {
        return instance.markers.stream().filter(m -> m.room() == room && m.kind().equals(kind)).toList();
    }

    /** Gate mobs of a room still alive. */
    private static int alive(ServerLevel level, Instance instance, int room) {
        if (room < 0 || room >= instance.rooms.size()) {
            return 0;
        }
        return level.getEntitiesOfClass(Mob.class, AABB.of(instance.rooms.get(room)).inflate(4.0),
            m -> m.isAlive() && GateSpawner.instanceOf(m) == instance.id && m.getPersistentData().getInt(GateSpawner.ROOM) == room).size();
    }

    /** `count` of the theme's mobs of `kind` at the room's spawn (else door) markers, `up` blocks above them. */
    private static void spawnAround(ServerLevel level, Instance instance, int room, GateMobs.Kind kind, int count, int up) {
        List<Marker> spots = new ArrayList<>();
        for (Marker m : instance.markers) {
            if (m.room() == room && (m.kind().startsWith("spawn:") || m.kind().equals("wave"))) {
                spots.add(m);
            }
        }
        if (spots.isEmpty()) {
            spots = markers(instance, room, "door");
        }
        if (spots.isEmpty()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Marker m = spots.get(level.random.nextInt(spots.size()));
            BlockPos pos = m.pos().above(up);
            while (up > 0 && !level.getBlockState(pos).isAir() && pos.getY() > m.pos().getY()) {
                pos = pos.below();
            }
            EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, kind, level.random);
            if (type != null) {
                GateSpawner.spawn(level, instance, type, pos, kind == GateMobs.Kind.ELITE ? GateSpawner.ELITE : null, room);
            }
        }
    }

    // ---- the tick ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        GateInstances gates = GateInstances.get(server);
        STATES.keySet().removeIf(id -> gates.byId(id) == null || !gates.byId(id).live());
        TRAPPED.keySet().removeIf(id -> gates.byId(id) == null || !gates.byId(id).live());
        int tick = server.getTickCount();
        for (Instance instance : new ArrayList<>(gates.all())) {
            ServerLevel level = server.getLevel(instance.dimension);
            if (level == null || instance.red || !instance.built || instance.state != GateInstances.State.ACTIVE) {
                continue;
            }
            List<ServerPlayer> present = GateCombat.present(level, instance);
            if (present.isEmpty()) {
                continue;
            }
            tick(level, instance, state(instance), present, tick);
        }
        if (tick % 200 == 0) {   // a ghost merchant of a closed gate goes with it
            for (ServerLevel level : server.getAllLevels()) {
                if (!GateDimensions.isGate(level.dimension())) {
                    continue;
                }
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof WanderingTrader trader && trader.getPersistentData().contains("mdvlcraft_gate_merchant")) {
                        Instance owner = gates.byId(trader.getPersistentData().getInt("mdvlcraft_gate_merchant"));
                        if (owner == null || !owner.live()) {
                            trader.discard();
                        }
                    }
                }
            }
        }
    }

    private static void tick(ServerLevel level, Instance instance, State state, List<ServerPlayer> present, int tick) {
        long now = level.getGameTime();
        for (Marker marker : instance.markers) {
            BlockPos pos = marker.pos();
            boolean near = present.stream().anyMatch(p -> p.blockPosition().distSqr(pos) < 24 * 24);
            if (!near) {
                continue;
            }
            switch (marker.kind()) {
                case "trap:flame" -> flame(level, pos, ((pos.getX() + pos.getZ()) & 1) == (tick / 40 & 1), tick);
                case "fountain", "altar", "cursed_shrine" -> shrine(level, instance, state, marker, present);
                case "ritual", "gauntlet" -> circle(level, instance, state, marker, present, now);
                default -> {
                }
            }
        }
        // portcullis: up again once its room's mobs are down
        for (Integer room : new ArrayList<>(state.barsDown)) {
            if (alive(level, instance, room) == 0) {
                state.barsDown.remove(room);
                for (Marker m : markers(instance, room, "bars")) {
                    for (int y = 0; y < 4; y++) {
                        if (level.getBlockState(m.pos().above(y)).is(Blocks.IRON_BARS)) {
                            level.setBlock(m.pos().above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    level.playSound(null, m.pos(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
                }
            }
        }
        // collapsing decks: a patch every 3 seconds
        state.collapseFrom.forEach((room, from) -> {
            if (now < from || (now - from) % 60 != 0) {
                return;
            }
            List<Marker> patches = markers(instance, room, "collapse");
            int done = state.collapsed.getOrDefault(room, 0);
            if (done < patches.size()) {
                state.collapsed.put(room, done + 1);
                BlockPos centre = patches.get(done).pos().below();
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
                    level.levelEvent(2001, p, Block.getId(level.getBlockState(p)));
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.playSound(null, centre, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
            }
        });
        // reinforcements
        for (Map.Entry<Integer, Integer> e : new ArrayList<>(state.reinforce.entrySet())) {
            int room = e.getKey();
            if (e.getValue() > 0 && alive(level, instance, room) <= e.getValue() / 2) {
                state.reinforce.remove(room);
                spawnAround(level, instance, room, GateMobs.Kind.MELEE, Math.max(2, e.getValue() / 2), 0);
                for (ServerPlayer p : present) {
                    if (instance.rooms.get(room).isInside(p.blockPosition())) {
                        p.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.reinforcements", "Reinforcements!")
                            .withStyle(ChatFormatting.RED), true);
                    }
                }
            }
        }
        // the lights go out
        for (Map.Entry<Integer, Long> e : new ArrayList<>(state.lightsOut.entrySet())) {
            if (now >= e.getValue()) {
                state.lightsOut.remove(e.getKey());
                for (ServerPlayer p : present) {
                    if (instance.rooms.get(e.getKey()).inflatedBy(2).isInside(p.blockPosition())) {
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        level.playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.8F, 0.5F);
                    }
                }
            }
        }
        if (tick % 20 == 0) {
            ambience(level, instance, present, tick);
        }
    }

    private static void flame(ServerLevel level, BlockPos pos, boolean burning, int tick) {
        if (!burning) {
            if (tick % 40 >= 30) {   // a warning puff before it fires
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.01);
            }
            return;
        }
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.8, 0.2, 0.02);
        if (tick % 40 == 0) {
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4F, 1.4F);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).expandTowards(0, 2, 0))) {
            e.setSecondsOnFire(3);
            e.hurt(level.damageSources().inFire(), 2.0F);
        }
    }

    private static void shrine(ServerLevel level, Instance instance, State state, Marker marker, List<ServerPlayer> present) {
        BlockPos pos = marker.pos();
        if (state.used.contains(pos)) {
            return;
        }
        ParticleOptions glow = marker.kind().equals("cursed_shrine") ? ParticleTypes.SOUL_FIRE_FLAME
            : marker.kind().equals("altar") ? ParticleTypes.END_ROD : ParticleTypes.SPLASH;
        level.sendParticles(glow, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.01);
        ServerPlayer user = present.stream().filter(p -> p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 1.6).findFirst().orElse(null);
        if (user == null) {
            return;
        }
        state.used.add(pos);
        switch (marker.kind()) {
            case "fountain" -> {
                user.setHealth(user.getMaxHealth());
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                level.sendParticles(ParticleTypes.HEART, user.getX(), user.getY(1.0), user.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
                user.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.fountain", "The fountain restores you.")
                    .withStyle(ChatFormatting.AQUA), true);
            }
            case "altar" -> {
                MobEffect blessing = BLESSINGS[level.random.nextInt(BLESSINGS.length)];
                for (ServerPlayer p : present) {
                    if (p.distanceToSqr(user) < 16 * 16) {
                        p.addEffect(new MobEffectInstance(blessing, 3600, blessing == MobEffects.DAMAGE_RESISTANCE ? 0 : 1));
                        p.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.altar", "A blessing: %s",
                            blessing.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
                    }
                }
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            default -> {
                state.cursedNext = true;
                GateRewards.lootChest(level, instance, pos, true);
                level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.6F);
                for (ServerPlayer p : present) {
                    p.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.cursed_shrine",
                        "The shrine pays you, and the next room will make you earn it.").withStyle(ChatFormatting.DARK_PURPLE), true);
                }
            }
        }
    }

    /** Ritual circle (three waves) and gauntlet (60 seconds): started by stepping into the middle. */
    private static void circle(ServerLevel level, Instance instance, State state, Marker marker, List<ServerPlayer> present, long now) {
        int room = marker.room();
        BlockPos pos = marker.pos();
        boolean gauntlet = marker.kind().equals("gauntlet");
        if (state.rewarded.contains(room)) {
            return;
        }
        boolean started = gauntlet ? state.gauntletUntil.containsKey(room) : state.wave.containsKey(room);
        if (!started) {
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 1.5, 0.2, 1.5, 0.5);
            if (present.stream().noneMatch(p -> p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 2.5)) {
                return;
            }
            if (alive(level, instance, room) > 0) {
                return;   // the room's own fight first
            }
            if (gauntlet) {
                state.gauntletUntil.put(room, now + 1200);
            } else {
                state.wave.put(room, 0);
            }
            level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
        int count = 2 + instance.rank.ordinal() / 2 + present.size();
        if (gauntlet) {
            long until = state.gauntletUntil.get(room);
            long left = until - now;
            if (left > 0 && left % 200 == 0) {
                spawnAround(level, instance, room, GateMobs.Kind.MELEE, count / 2 + 1, 0);
                spawnAround(level, instance, room, GateMobs.Kind.RANGED, 1, 0);
            }
            if (left % 20 == 0) {
                for (ServerPlayer p : present) {
                    if (instance.rooms.get(room).isInside(p.blockPosition())) {
                        p.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.gauntlet", "Survive: %s s",
                            Math.max(0, left / 20)).withStyle(ChatFormatting.GOLD), true);
                    }
                }
            }
            if (left <= 0) {
                reward(level, instance, state, room);
            }
            return;
        }
        int wave = state.wave.get(room);
        if (alive(level, instance, room) == 0) {
            if (wave >= 3) {
                reward(level, instance, state, room);
            } else {
                state.wave.put(room, wave + 1);
                spawnAround(level, instance, room, GateMobs.Kind.MELEE, count, 0);
                spawnAround(level, instance, room, wave == 2 ? GateMobs.Kind.ELITE : GateMobs.Kind.RANGED, 1, 0);
                level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        }
    }

    private static void reward(ServerLevel level, Instance instance, State state, int room) {
        if (!state.rewarded.add(room)) {
            return;
        }
        for (Marker m : markers(instance, room, "reward")) {
            GateRewards.lootChest(level, instance, m.pos(), true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.pos().getX() + 0.5, m.pos().getY() + 1.0, m.pos().getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.0);
        }
        level.playSound(null, instance.rooms.get(room).getCenter(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    // ---- ambience: each mood's particles and sounds ----

    private static void ambience(ServerLevel level, Instance instance, List<ServerPlayer> present, int tick) {
        GatePalettes.Mood mood = GatePalettes.style(instance).mood();
        ParticleOptions particle = switch (mood) {
            case FLOODED -> ParticleTypes.DRIPPING_WATER;
            case OVERGROWN -> ParticleTypes.FALLING_SPORE_BLOSSOM;
            case RUINED -> ParticleTypes.WHITE_ASH;
            case HAUNTED -> ParticleTypes.SOUL;
            case DARK -> ParticleTypes.SMOKE;
            default -> ParticleTypes.MYCELIUM;
        };
        SoundEvent sound = switch (mood) {
            case FLOODED -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            case OVERGROWN -> SoundEvents.AZALEA_LEAVES_STEP;
            case RUINED -> SoundEvents.GRAVEL_FALL;
            case HAUNTED -> SoundEvents.SOUL_ESCAPE;
            case DARK -> SoundEvents.AMBIENT_CAVE.value();
            default -> SoundEvents.CHAIN_STEP;
        };
        for (ServerPlayer p : present) {
            level.sendParticles(p, particle, false, p.getX(), p.getY() + 2.5, p.getZ(), 10, 6.0, 2.0, 6.0, 0.0);
            if (level.random.nextInt(25) == 0) {
                p.playNotifySound(sound, SoundSource.AMBIENT, 0.5F, 0.6F + level.random.nextFloat() * 0.4F);
            }
        }
    }

    // ---- interactions ----

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GateDimensions.isGate(level.dimension())
            || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        Instance instance = GateInstances.get(level.getServer()).at(level.dimension(), pos.getX(), pos.getZ());
        if (instance == null) {
            return;
        }
        Set<BlockPos> trapped = TRAPPED.get(instance.id);
        if (trapped != null && trapped.remove(pos)) {
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.5F);
            player.displayClientMessage(Component.translatableWithFallback("gate.mdvlcraft.trapped_chest", "It's a trap!")
                .withStyle(ChatFormatting.RED), true);
            for (int i = 0; i < 2 + instance.rank.ordinal() / 2; i++) {
                EntityType<?> type = GateMobs.pick(instance.theme, instance.rank, i == 0 ? GateMobs.Kind.ELITE : GateMobs.Kind.MELEE, level.random);
                BlockPos at = pos.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
                if (type != null && level.getBlockState(at).isAir()) {
                    GateSpawner.spawn(level, instance, type, at, i == 0 ? GateSpawner.ELITE : null, -1);
                }
            }
            return;
        }
        for (Marker marker : instance.markers) {
            if (marker.kind().equals("lever") && marker.pos().equals(pos)) {
                lever(level, instance, marker, player);
                return;
            }
        }
    }

    private static void lever(ServerLevel level, Instance instance, Marker marker, ServerPlayer player) {
        State state = state(instance);
        int room = marker.room();
        if (state.rewarded.contains(room)) {
            return;
        }
        List<Marker> levers = markers(instance, room, "lever");
        int index = levers.indexOf(marker);
        int[] order = state.leverOrder.computeIfAbsent(room, r -> {
            List<Integer> l = new ArrayList<>();
            for (int i = 0; i < levers.size(); i++) {
                l.add(i);
            }
            java.util.Collections.shuffle(l, new Random(instance.id * 17L + r));
            return l.stream().mapToInt(Integer::intValue).toArray();
        });
        List<Integer> pulls = state.pulls.computeIfAbsent(room, r -> new ArrayList<>());
        if (pulls.contains(index)) {
            return;
        }
        pulls.add(index);
        BlockPos pos = marker.pos();
        if (order[pulls.size() - 1] != index) {
            pulls.clear();
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 0.6F);
            spawnAround(level, instance, room, GateMobs.Kind.MELEE, 2, 0);
            for (Marker m : levers) {   // throw them all back
                BlockState s = level.getBlockState(m.pos());
                if (s.getBlock() instanceof LeverBlock) {
                    level.setBlock(m.pos(), s.setValue(LeverBlock.POWERED, false), Block.UPDATE_ALL);
                }
            }
            return;
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 0.8F + 0.2F * pulls.size());
        if (pulls.size() == levers.size()) {
            reward(level, instance, state, room);
        }
    }

    // ---- the treasure goblin ----

    private static void goblin(ServerLevel level, Instance instance, int room) {
        List<Marker> spots = markers(instance, room, "spawn:melee");
        if (spots.isEmpty()) {
            return;
        }
        net.minecraft.world.entity.monster.Zombie goblin = EntityType.ZOMBIE.create(level);   // a baby: small and quick
        if (goblin == null) {
            return;
        }
        goblin.setBaby(true);
        BlockPos pos = spots.get(0).pos();
        goblin.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        goblin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        goblin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        goblin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT));
        goblin.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.treasure_goblin", "Treasure Goblin").withStyle(ChatFormatting.GOLD));
        goblin.setCustomNameVisible(true);
        goblin.setGlowingTag(true);
        goblin.setPersistenceRequired();
        var speed = goblin.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addPermanentModifier(new AttributeModifier(java.util.UUID.fromString("2b8e6c1a-4f0d-4c3b-9a71-5e2d8f0c6a14"), "goblin", 0.6,
                AttributeModifier.Operation.MULTIPLY_BASE));
        }
        goblin.getPersistentData().putInt("mdvlcraft_goblin", instance.id);
        goblin.getPersistentData().putLong("mdvlcraft_goblin_until", level.getGameTime() + 600);
        level.addFreshEntity(goblin);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.0F, 1.6F);
    }

    /** The goblin escapes after 30 seconds; caught, it drops a vault's worth. */
    @SubscribeEvent
    public static void onGoblinTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !e.getPersistentData().contains("mdvlcraft_goblin") || e.tickCount % 10 != 0) {
            return;
        }
        if (e.level().getGameTime() >= e.getPersistentData().getLong("mdvlcraft_goblin_until")) {
            ((ServerLevel)e.level()).sendParticles(ParticleTypes.POOF, e.getX(), e.getY(0.5), e.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            e.discard();
            return;
        }
        if (e instanceof Mob mob && mob.getTarget() != null) {   // it runs from whoever chases it
            var away = mob.position().subtract(mob.getTarget().position()).normalize().scale(6.0).add(mob.position());
            mob.setTarget(null);
            mob.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
        }
    }

    @SubscribeEvent
    public static void onGoblinDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level() instanceof ServerLevel champLevel && e.getPersistentData().getBoolean("mdvlcraft_champion")) {
            Instance owner = GateInstances.get(champLevel.getServer()).byId(GateSpawner.instanceOf(e));
            if (owner != null) {   // the champion's room pays out
                reward(champLevel, owner, state(owner), e.getPersistentData().getInt(GateSpawner.ROOM));
            }
        }
        if (!(e.level() instanceof ServerLevel level) || !e.getPersistentData().contains("mdvlcraft_goblin")) {
            return;
        }
        Instance instance = GateInstances.get(level.getServer()).byId(e.getPersistentData().getInt("mdvlcraft_goblin"));
        BlockPos at = e.blockPosition();
        if (instance != null && level.getBlockState(at).isAir()) {
            GateRewards.lootChest(level, instance, at, true);
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), e.getY(0.5), e.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
    }

    @Nullable
    static Mob champion(Mob mob) {
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addPermanentModifier(new AttributeModifier(java.util.UUID.fromString("7c4a1e90-2d6b-4f35-8e1c-9b0a3d5f7e21"), "champion", 1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        mob.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.champion", "Champion").withStyle(ChatFormatting.RED));
        mob.setCustomNameVisible(true);
        return mob;
    }
}
