package com.mdvlcraft.binder.ability;

import com.bless.naoyaaddon.entity.ModEntities;
import com.bless.naoyaaddon.entity.ProjectionFrameEntity;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.PhantomRushPacket;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.api.event.OnExecutionStartEvent;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.execution.ExecutionHandler.ExecutionTransform;
import net.shelmarow.combat_evolution.execution.ExecutionTask;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import net.shelmarow.combat_evolution.tickTask.TickTaskManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Phantom Movement's second mode, Assassination Rush (sneak and cast to switch). Cast on three creatures one at a time,
 * then the caster dashes a Projection frame trail to each in turn at three times Phantom Movement's speed and assassinates
 * it with Combat Evolution's assassination, played at double speed (mixin/AnimationPlayerSpeedMixin).
 */
public final class PhantomRush {
    private static final int TARGETS = 3;
    /** Phantom Movement maps 24 frames 2 blocks apart. */
    private static final double RANGE = 48.0;
    private static final double FRAME_GAP = 2.0;
    /** Phantom Movement dashes one frame a tick; the rush three. */
    private static final int FRAMES_PER_TICK = 3;
    private static final int COOLDOWN_TICKS = 600;
    private static final int PICK_TICKS = 400;
    private static final String READY = "mdvlcraft:phantom_rush_ready";
    public static final String ICON_SPELL = "irons_spellbooks:flaming_barrage";

    private static final Set<UUID> MODE = new HashSet<>();
    private static final Map<UUID, Rush> RUSHES = new HashMap<>();
    /** Executor id to target id of rush assassinations in progress, whose animations play at double speed, per side. */
    public static final Map<Integer, Integer> SERVER_FAST = new ConcurrentHashMap<>();
    public static final Map<Integer, Integer> CLIENT_FAST = new ConcurrentHashMap<>();

    private PhantomRush() {
    }

    public static float speed(LivingEntity entity) {
        Map<Integer, Integer> fast = entity.level().isClientSide() ? CLIENT_FAST : SERVER_FAST;
        return fast.isEmpty() || !fast.containsKey(entity.getId()) && !fast.containsValue(entity.getId()) ? 1.0F : 2.0F;
    }

    /** Whether this cast of Phantom Movement belongs to the rush (sneaking toggles; in rush mode every cast picks). */
    static boolean handles(ServerPlayer player) {
        return player.isShiftKeyDown() || MODE.contains(player.getUUID());
    }

    static void press(ServerPlayer player, int cost) {
        Rush rush = RUSHES.computeIfAbsent(player.getUUID(), id -> new Rush());
        if (rush.index >= 0) {
            return;
        }

        long now = player.level().getGameTime();
        if (player.isShiftKeyDown() && MODE.contains(player.getUUID()) && !rush.targets.isEmpty()) {
            launch(player, rush, cost, now);   // sneaking with 1-2 picked: rush them now instead of switching mode
            return;
        }

        if (player.isShiftKeyDown()) {
            boolean on = MODE.add(player.getUUID()) || !MODE.remove(player.getUUID());
            rush.targets.clear();
            sync(player, -1, rush);
            bar(player, Component.translatable(on ? "ability.mdvlcraft.phantom_movement.rush" : "ability.mdvlcraft.phantom_movement.normal")
                .withStyle(ChatFormatting.BLUE));
            return;
        }

        long left = player.getPersistentData().getLong(READY) - now;
        if (left > 0L) {
            bar(player, Component.translatable("ability.mdvlcraft.on_cooldown", Component.translatable("ability.mdvlcraft.phantom_rush"), (left + 19L) / 20L)
                .withStyle(ChatFormatting.RED));
            return;
        }

        LivingEntity target = lookedAt(player, rush.targets);
        if (target == null) {
            return;
        }

        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        ServerPlayerPatch self = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null || self == null || !ExecutionHandler.isTargetSupported(self, patch)) {
            bar(player, Component.translatable("ability.mdvlcraft.phantom_rush.unfit").withStyle(ChatFormatting.RED));
            return;
        }

        rush.targets.add(target);
        rush.pickUntil = now + PICK_TICKS;
        bar(player, Component.translatable("ability.mdvlcraft.phantom_rush.picked", rush.targets.size(), TARGETS).withStyle(ChatFormatting.BLUE));
        if (rush.targets.size() < TARGETS) {
            sync(player, -1, rush);
            return;
        }

        launch(player, rush, cost, now);
    }

    /** Pays and starts the rush on the picked targets (3, or fewer when the player sneak-casts early). */
    private static void launch(ServerPlayer player, Rush rush, int cost, long now) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (!player.isCreative()) {
            if (magic.getMana() < cost) {
                bar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", Technique.PHANTOM_MOVEMENT.displayName()).withStyle(ChatFormatting.RED));
                rush.targets.clear();
                sync(player, -1, rush);
                return;
            }

            magic.setMana(magic.getMana() - cost);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        }

        player.getPersistentData().putLong(READY, now + Math.round(COOLDOWN_TICKS * TechniqueCooldowns.cooldownMultiplier(player)));
        rush.index = 0;
        sync(player, -1, null);
    }

    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Rush>> entries = RUSHES.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry<UUID, Rush> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Rush rush = entry.getValue();
            if (player == null || !player.isAlive()) {
                SERVER_FAST.remove(rush.executor);
                entries.remove();
            } else if (rush.index < 0) {
                if (!rush.targets.isEmpty() && player.level().getGameTime() > rush.pickUntil) {
                    rush.targets.clear();
                    sync(player, -1, rush);
                }
            } else if (!step(player, rush)) {
                entries.remove();
            }
        }
    }

    /** One tick of a running rush; false once every target is done. */
    private static boolean step(ServerPlayer player, Rush rush) {
        if (rush.wait > 0 && --rush.wait > 0) {
            return true;
        }

        if (rush.wait == 0 && SERVER_FAST.remove(player.getId()) != null) {
            sync(player, -1, null);
            rush.index++;
            rush.path = null;
        }

        if (rush.path == null) {
            while (rush.index < rush.targets.size() && !valid(player, rush.targets.get(rush.index))) {
                rush.index++;
            }

            if (rush.index >= rush.targets.size()) {
                return false;
            }

            rush.path = trail(player, rush.targets.get(rush.index));
            rush.step = 0;
        }

        LivingEntity target = rush.targets.get(rush.index);
        rush.step = Math.min(rush.step + FRAMES_PER_TICK, rush.path.size());
        Vec3 at = rush.path.get(rush.step - 1);
        Vec3 look = target.position().subtract(at);
        player.teleportTo(at.x, at.y, at.z);
        player.setYRot((float)(Mth.atan2(look.z, look.x) * (180.0 / Math.PI)) - 90.0F);
        player.setYHeadRot(player.getYRot());
        player.fallDistance = 0.0F;
        if (rush.step < rush.path.size()) {
            return true;
        }

        int ticks = valid(player, target) ? assassinate(player, target) : 0;
        if (ticks > 0) {
            rush.wait = ticks;
            rush.executor = player.getId();
            SERVER_FAST.put(player.getId(), target.getId());
            sync(player, target.getId(), null);
        } else {
            rush.index++;
            rush.path = null;
        }

        return true;
    }

    /**
     * The creature under the crosshair within RANGE, skipping those already picked. CE's getEntityLookedAt (made for its
     * 2-block assassination reach) returns the nearest creature on the ray, so a picked one standing near the line of sight
     * hid the next, and it also needed a clear line to the creature's eyes; here the ray just stops at the first block.
     */
    private static LivingEntity lookedAt(ServerPlayer player, List<LivingEntity> picked) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1.0F).scale(RANGE)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player)).getLocation();
        EntityHitResult hit = ExecutionHandler.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1.0),
            entity -> entity instanceof LivingEntity living && !picked.contains(living) && living.isAlive() && !living.isInvulnerable()
                && player.canAttack(living, TargetingConditions.DEFAULT),
            eye.distanceToSqr(end), 1.0);
        return hit == null ? null : (LivingEntity)hit.getEntity();
    }

    private static boolean valid(ServerPlayer player, LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && target.level() == player.level();
    }

    /** Phantom Movement's frames, 2 blocks apart, from the caster to just behind the target. */
    private static List<Vec3> trail(ServerPlayer player, LivingEntity target) {
        Vec3 start = player.position();
        Vec3 end = target.position().subtract(Vec3.directionFromRotation(0.0F, target.getYRot()).scale(target.getBbWidth() / 2.0 + 0.6));
        Vec3 delta = end.subtract(start);
        float yaw = (float)(Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        int frames = Math.max(1, Mth.ceil(delta.length() / FRAME_GAP));
        List<Vec3> path = new ArrayList<>(frames);

        for (int i = 1; i <= frames; i++) {
            Vec3 at = start.add(delta.scale((double)i / frames));
            path.add(at);
            ProjectionFrameEntity frame = new ProjectionFrameEntity(ModEntities.PROJECTION_FRAME.get(), player.level());
            frame.setPos(at.x, at.y, at.z);
            frame.setOwnerRot(yaw, 0.0F);
            frame.setFrozen(true);
            frame.setFrameIndex(i);
            frame.setOwnerId(player.getId());
            player.level().addFreshEntity(frame);
        }

        return path;
    }

    /** Combat Evolution's assassination (ExecutionHandler.tryAssassinate) on a chosen target, at double speed; its length in ticks, 0 if it can't. */
    private static int assassinate(ServerPlayer player, LivingEntity target) {
        ServerPlayerPatch self = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (self == null || patch == null || !ExecutionHandler.canExecute(player, self, target, patch)) {
            return 0;
        }

        ExecutionTypeManager.Type type = ExecutionTypeManager.getAssassinationType(self, patch);
        ExecutionTransform transform = type == null ? null : ExecutionHandler.calculateExecutionPosition(player.level(), player, target, type.offset());
        if (transform == null || MinecraftForge.EVENT_BUS.post(new OnExecutionStartEvent(self, patch, type))) {
            return 0;
        }

        Vec3 look = target.position().subtract(transform.position());
        transform = new ExecutionTransform(transform.position(), (float)Math.toDegrees(Mth.atan2(look.z, look.x)) - 90.0F);
        target.setDeltaMovement(Vec3.ZERO);
        BehaviorUtils.stopCurrentBehavior(target);
        player.teleportTo(transform.position().x, transform.position().y, transform.position().z);
        int ticks = Math.max(1, type.totalTick() / 2);
        TickTaskManager.addTask(target.getUUID(), new ExecutionTask(player, target, type, transform, ticks));
        return ticks;
    }

    /** Tells the caster's client which targets are picked (Iron's multi-target recast icon) and who is mid-assassination. */
    private static void sync(ServerPlayer player, int fastTarget, Rush rush) {
        List<UUID> picked = new ArrayList<>();
        if (rush != null) {
            rush.targets.forEach(target -> picked.add(target.getUUID()));
        }

        BinderNetwork.sendToTrackingAndSelf(player, new PhantomRushPacket(player.getId(), fastTarget, picked, TARGETS));
    }

    private static void bar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private static final class Rush {
        final List<LivingEntity> targets = new ArrayList<>();
        long pickUntil;
        /** Target being rushed; -1 while picking. */
        int index = -1;
        List<Vec3> path;
        int step;
        int wait;
        int executor;
    }
}
