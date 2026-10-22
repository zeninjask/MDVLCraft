package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.RedLandscape.Layout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Red S gates (plan 9.2, late-game challenge 2). On top of an ordinary red gate (RedGates, RedLandscape) a red S gate has:
 *   - two extra camps by the lair (LAIR_CAMPS, 20–24 blocks from the arena centre), added to its layout;
 *   - two mana crystal veins at every camp once it is built (red gates otherwise get none in the red realm), so "double";
 *   - always a hidden boss: when the lair boss dies, the seal holds and a second boss rises in the arena (another theme's S
 *     boss when its mod is loaded, else the same, with HIDDEN_HEALTH times the health). Its death breaks the seal.
 * Only the hidden boss drops black essence stones (GateRewards asks {@link #dropsBlack}), which makes red S gates their only
 * source. Per-instance progress is in memory, like RedLandscape's layouts.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class HiddenBoss {
    /** Persistent NBT boolean on a hidden boss. */
    public static final String TAG = "mdvlcraft_gate_hidden";
    static final int LAIR_CAMPS = 2;
    static final int VEINS_PER_CAMP = 2;
    private static final double HIDDEN_HEALTH = 1.5;
    private static final UUID HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0301");
    /** Red S instances whose layout has its lair camps. */
    private static final Set<Integer> PLANNED = new HashSet<>();
    /** Camps given their veins, per instance. */
    private static final Map<Integer, Set<Integer>> VEINED = new HashMap<>();
    /** Instances whose hidden boss has risen. */
    private static final Set<Integer> RISEN = new HashSet<>();

    private HiddenBoss() {
    }

    static boolean applies(Instance instance) {
        return instance.red && instance.rank == GateRank.S;
    }

    /** GateRewards.onDrops: black essence stones come only from a hidden boss. */
    public static boolean dropsBlack(LivingEntity mob) {
        return mob.getPersistentData().getBoolean(TAG);
    }

    /**
     * RedGates.onDeath, when a red gate's boss dies: in a red S gate whose hidden boss hasn't risen, it rises now and the
     * seal holds (returns true; the caller then does nothing more).
     */
    public static boolean rise(ServerLevel level, Instance instance, Layout layout, LivingEntity dead) {
        if (!applies(instance) || dropsBlack(dead) || !RISEN.add(instance.id)) {
            return false;
        }
        BlockPos floor = layout.lairFloor != null ? layout.lairFloor : (layout.lairFloor = RedLandscape.buildArena(level, instance, layout.lair));
        Mob boss = spawn(level, instance, floor);
        if (boss == null) {
            RISEN.remove(instance.id);
            return false;   // nothing could rise: the seal breaks as usual
        }
        for (ServerPlayer player : RedGates.inside(level.getServer(), instance)) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.hidden.subtitle",
                "The seal holds")));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("gate.mdvlcraft.hidden.title",
                "Something else stirs").withStyle(ChatFormatting.DARK_RED)));
            player.sendSystemMessage(Component.translatableWithFallback("gate.mdvlcraft.hidden.message",
                "A hidden boss rises in the lair. The gate stays sealed until it falls.").withStyle(ChatFormatting.RED));
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        return true;
    }

    /** The hidden boss in the arena, flagged as the lair boss (RedLandscape.BOSS_TAG) and with TAG. */
    @Nullable
    static Mob spawn(ServerLevel level, Instance instance, BlockPos floor) {
        EntityType<?> type = null;
        List<GateTheme> others = new ArrayList<>(List.of(GateTheme.values()));
        others.remove(instance.theme);
        while (type == null && !others.isEmpty()) {
            type = GateMobs.type(GateMobs.boss(others.remove(level.random.nextInt(others.size())), instance.rank).id());
        }
        if (type == null) {
            type = GateMobs.type(GateMobs.boss(instance.theme, instance.rank).id());
        }
        if (type == null) {
            type = RedLandscape.type(RedLandscape.standInBoss(instance.theme));
        }
        Mob boss = type == null ? null : GateSpawner.spawn(level, instance, type, floor.offset(0, 1, -6), GateSpawner.BOSS, -1);
        if (boss == null) {
            return null;
        }
        boss.getPersistentData().putBoolean(RedLandscape.BOSS_TAG, true);
        boss.getPersistentData().putBoolean(TAG, true);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH) == null) {
            health.addPermanentModifier(new AttributeModifier(HEALTH, "hidden boss", HIDDEN_HEALTH - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        boss.setHealth(boss.getMaxHealth());
        boss.setGlowingTag(true);
        boss.setCustomName(Component.translatableWithFallback("gate.mdvlcraft.hidden.name", "Hidden %s", type.getDescription())
            .withStyle(ChatFormatting.DARK_RED));
        return boss;
    }

    /** Adds the lair camps to new red S layouts and puts veins at camps RedGates has built. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 10) {
            return;
        }
        MinecraftServer server = event.getServer();
        GateInstances gates = GateInstances.get(server);
        for (Layout layout : RedLandscape.LAYOUTS.values()) {
            Instance instance = gates.byId(layout.instanceId);
            ServerLevel level = instance == null ? null : server.getLevel(instance.dimension);
            if (level == null || !applies(instance)) {
                continue;
            }
            if (PLANNED.add(instance.id)) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                for (int n = 0; n < LAIR_CAMPS; n++) {
                    double a = angle + Math.PI * n;   // opposite sides of the arena
                    int d = RedLandscape.ARENA_RADIUS + 6 + level.random.nextInt(5);
                    layout.camps.add(layout.lair.offset((int)(Math.cos(a) * d), 0, (int)(Math.sin(a) * d)));
                }
            }
            Set<Integer> veined = VEINED.computeIfAbsent(instance.id, id -> new HashSet<>());
            for (int n : layout.campsDone) {
                if (veined.add(n)) {
                    veins(level, instance, layout.camps.get(n));
                }
            }
        }
        PLANNED.removeIf(id -> gates.byId(id) == null);
        VEINED.keySet().removeIf(id -> gates.byId(id) == null);
        RISEN.removeIf(id -> gates.byId(id) == null);
    }

    /** VEINS_PER_CAMP veins on the ground a few blocks out from the camp, red-sized (6–8 ore) and scaled by the modifiers. */
    static void veins(ServerLevel level, Instance instance, BlockPos camp) {
        var ore = GateRewards.CRYSTAL_ORES.get(instance.rank).get().defaultBlockState();
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        for (int n = 0; n < VEINS_PER_CAMP; n++) {
            double a = angle + Math.PI * 2.0 * n / VEINS_PER_CAMP;
            BlockPos at = RedLandscape.surface(level, camp.offset((int)(Math.cos(a) * 6), 0, (int)(Math.sin(a) * 6)));
            int size = GateModifiers.scale(6 + level.random.nextInt(3), GateModifiers.rewardFactor(instance), level.random);
            GateRewards.vein(level, at, ore, size, level.random);
        }
    }
}
