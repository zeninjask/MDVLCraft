package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Spawns gate mobs (plan 5.3). Mobs are made the way integration/BossArena does it, calling finalizeSpawn directly, so the
 * pack's content filter (which cancels natural/event spawns of many mob mods) doesn't stop them. Every gate mob is scaled
 * by its gate's rank and carries its instance id in persistent NBT ({@link #TAG}); rewards, clear checks and breaks find
 * gate mobs by that tag. Each also records its role ({@link #ROLE}: elite or boss), its room ({@link #ROOM}, an index into
 * the instance's rooms) and where it was spawned ({@link #HOME}), so GateCombat can find the boss and pull strays back.
 * Elites get twice the health and a glowing outline. Which mobs spawn is GateMobs' table; when, GateCombat's room triggers.
 */
public final class GateSpawner {
    /** Persistent NBT int on every gate mob: the id of its instance. */
    public static final String TAG = "mdvlcraft_gate";
    /** Persistent NBT string: {@link #ELITE} or {@link #BOSS} (absent on ordinary gate mobs). */
    public static final String ROLE = GateRewards.ROLE;
    /** Persistent NBT int: the room the mob belongs to (-1: none). */
    public static final String ROOM = "mdvlcraft_gate_room";
    /** Persistent NBT long: the block it was spawned at. */
    public static final String HOME = "mdvlcraft_gate_home";
    public static final String ELITE = "elite";
    public static final String BOSS = "boss";
    private static final UUID HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0001");
    private static final UUID DAMAGE = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0002");
    private static final UUID ARMOUR = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0003");
    private static final UUID ELITE_HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0004");
    private static final UUID BOSS_ARMOUR = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0005");
    private static final UUID BOSS_TOUGHNESS = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0006");
    /** The share of the rank's health bonus a boss keeps. */
    private static final double BOSS_HEALTH_SHARE = 0.5;
    /** The share of a boss's own armour and toughness taken off. */
    private static final double BOSS_DEFENCE_CUT = 0.5;
    private static final UUID BOSS_EXTRA_HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0007");
    private static final UUID BOSS_TARGET_HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0008");
    private static final UUID BOSS_MIN_DAMAGE = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0009");
    private static final UUID BOSS_RANK_HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a000a");
    private static final UUID BOSS_RANK_DAMAGE = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a000b");
    private static final UUID BOSS_ARMOUR_CAP_ID = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a000c");
    private static final UUID BOSS_SPEED_ID = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a000d");
    /** Each boss is brought up to its rank's health (E..S) whatever its own base health, so weak bosses aren't pushovers. */
    private static final double[] BOSS_HEALTH = {150.0, 300.0, 550.0, 900.0, 1500.0, 2500.0};
    /** And hits at least this hard (E..S). */
    private static final double[] BOSS_DAMAGE = {6.0, 9.0, 12.0, 16.0, 22.0, 30.0};

    private GateSpawner() {
    }

    /** A mob of `type` at `pos`, scaled to the instance's rank and tagged with it; null if the type can't be made. */
    @Nullable
    public static Mob spawn(ServerLevel level, Instance instance, EntityType<?> type, BlockPos pos) {
        return spawn(level, instance, type, pos, null, -1);
    }

    /** As above, with a role (null, {@link #ELITE} or {@link #BOSS}) and the room it belongs to (-1 for none). */
    @Nullable
    public static Mob spawn(ServerLevel level, Instance instance, EntityType<?> type, BlockPos pos, @Nullable String role, int room) {
        if (!(type.create(level) instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        scale(mob, instance.rank);
        if (BOSS.equals(role)) {
            soften(mob);
            bossTargets(mob, instance.rank);
            bossHealth(mob, instance);
            hardRank(mob, instance.rank);
        }
        if (ELITE.equals(role)) {
            modify(mob.getAttribute(Attributes.MAX_HEALTH), ELITE_HEALTH, "gate elite", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
            mob.setHealth(mob.getMaxHealth());
            mob.setGlowingTag(true);
        }
        CompoundTag data = mob.getPersistentData();
        data.putInt(TAG, instance.id);
        if (role != null) {
            data.putString(ROLE, role);
        }
        if (BOSS.equals(role)) {
            data.putBoolean(RedLandscape.BOSS_TAG, true);   // the same flag red gates use for their boss
        }
        data.putInt(ROOM, room);
        data.putLong(HOME, pos.asLong());
        mob.addTag(TAG);   // for selectors (@e[tag=mdvlcraft_gate]) in tests and admin commands
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /** The instance id a gate mob belongs to, or -1 for any other entity. */
    public static int instanceOf(Entity entity) {
        return entity.getPersistentData().contains(TAG) ? entity.getPersistentData().getInt(TAG) : -1;
    }

    /** True for a gate mob spawned as its gate's boss. */
    public static boolean isBoss(Entity entity) {
        return BOSS.equals(entity.getPersistentData().getString(ROLE));
    }

    /** Every rank's bonus (health, damage, armour above an unscaled mob) is taken at four fifths. */
    private static final double SCALING = 0.8;

    static void scale(Mob mob, GateRank rank) {
        modify(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH, "gate health", (rank.health - 1.0) * SCALING, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, "gate damage", (rank.damage - 1.0) * SCALING, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ARMOR), ARMOUR, "gate armour", rank.armour * SCALING, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
    }

    /**
     * A boss's own defences are already a boss's (and most carry their mod's own damage and DPS caps): the rank's armour bonus
     * is dropped, its health bonus halved, and its own armour and toughness halved, so a fitting party can wear it down.
     */
    private static void soften(Mob mob) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier bonus = health == null ? null : health.getModifier(HEALTH);
        if (bonus != null) {
            health.removeModifier(HEALTH);
            health.addPermanentModifier(new AttributeModifier(HEALTH, "gate health", bonus.getAmount() * BOSS_HEALTH_SHARE, bonus.getOperation()));
        }
        AttributeInstance armour = mob.getAttribute(Attributes.ARMOR);
        if (armour != null) {
            armour.removeModifier(ARMOUR);
        }
        modify(armour, BOSS_ARMOUR, "gate boss armour", -BOSS_DEFENCE_CUT, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ARMOR_TOUGHNESS), BOSS_TOUGHNESS, "gate boss toughness", -BOSS_DEFENCE_CUT, AttributeModifier.Operation.MULTIPLY_TOTAL);
        mob.setHealth(mob.getMaxHealth());
    }

    /** Most armour a boss keeps (after soften): above it hits barely land (Sukuna has 42). */
    private static final double BOSS_ARMOUR_CAP = 12.0;
    /** A melee boss slower than this walks 20% faster, so it can't simply be kited. */
    private static final double SLOW_BOSS = 0.2;
    /** Casters that keep their distance on purpose: not sped up. */
    private static final java.util.Set<String> CASTERS = java.util.Set.of("irons_spellbooks:dead_king", "traveloptics:enraged_dead_king",
        "irons_spellbooks:fire_boss", "traveloptics:aqua_grandmaster");

    /**
     * A boss below its rank's health or damage (BOSS_HEALTH, BOSS_DAMAGE) is raised to it; stronger bosses keep theirs. Every
     * gate boss (normal, red, hidden, Demon Castle) also has its armour capped at BOSS_ARMOUR_CAP, a slow melee boss is sped
     * up, and Cursed Fate's NPCs count their domain as used: a Malevolent Shrine would rebuild the arena around the fight.
     */
    public static void bossTargets(Mob mob, GateRank rank) {
        AttributeInstance armour = mob.getAttribute(Attributes.ARMOR);
        if (armour != null && armour.getValue() > BOSS_ARMOUR_CAP) {
            modify(armour, BOSS_ARMOUR_CAP_ID, "gate boss armour cap", BOSS_ARMOUR_CAP - armour.getValue(), AttributeModifier.Operation.ADDITION);
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        String id = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
        if (speed != null && speed.getBaseValue() < SLOW_BOSS && !CASTERS.contains(id)) {
            modify(speed, BOSS_SPEED_ID, "gate boss speed", 0.2, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        mob.getPersistentData().putBoolean("DomainUsed", true);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        double target = BOSS_HEALTH[rank.ordinal()];
        if (health != null && health.getValue() < target) {
            modify(health, BOSS_TARGET_HEALTH, "gate boss rank health", target / health.getValue() - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double least = BOSS_DAMAGE[rank.ordinal()];
        if (damage != null && damage.getValue() < least) {
            modify(damage, BOSS_MIN_DAMAGE, "gate boss rank damage", least - damage.getValue(), AttributeModifier.Operation.ADDITION);
        }
        mob.setHealth(mob.getMaxHealth());
    }

    /** A and S bosses hit harder and last longer: +15% health and damage at A, +20% at S. */
    private static void hardRank(Mob mob, GateRank rank) {
        double extra = rank == GateRank.S ? 0.2 : rank == GateRank.A ? 0.15 : 0.0;
        modify(mob.getAttribute(Attributes.MAX_HEALTH), BOSS_RANK_HEALTH, "gate boss A/S health", extra, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE), BOSS_RANK_DAMAGE, "gate boss A/S damage", extra, AttributeModifier.Operation.MULTIPLY_TOTAL);
        mob.setHealth(mob.getMaxHealth());
    }

    /** The theme file's per-boss health multiplier for this rank, when it is not 1. */
    private static void bossHealth(Mob mob, Instance instance) {
        GateThemes.Theme data = GateThemes.get(instance.theme);
        double health = data == null ? 1.0 : data.bosses()[instance.rank.ordinal()].health();
        if (health != 1.0 && data.bosses()[instance.rank.ordinal()].ids().stream().anyMatch(id -> GateMobs.type(id) == mob.getType())) {
            modify(mob.getAttribute(Attributes.MAX_HEALTH), BOSS_EXTRA_HEALTH, "gate boss health", health - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void modify(@Nullable AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute != null && amount != 0.0 && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        }
    }
}
