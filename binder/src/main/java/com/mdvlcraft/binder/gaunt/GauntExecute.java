package com.mdvlcraft.binder.gaunt;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.GauntActionPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Execute (ability/GauntAbility.EXECUTE): the Haunted Lord seizes the creature it looks at by the neck, lifts it off its
 * feet, holds it kicking, and snaps its neck (the head flops to the side), then lets the body drop. Works on anything
 * about player-sized (not bosses) at or below half health, within REACH.
 *
 * The Haunted Lord plays its execute animation (art/haunted_lord/source/anims.py: execute, grip); the victim is held at
 * the right hand's knuckles all along it: this side moves it there every tick, and every client places it there every
 * frame (client/GauntClient) from the same path (victimAt), turning its head aside after the snap. Danielrutterfilm's
 * "Necksnap_build" (haunted_lord.neck_snap) starts SNAP_SOUND_LEAD ticks before the snap, so its crack lands on it. The
 * victim dies at DROP_TICK. Both are sent to everyone tracking the Haunted Lord (GauntActionPacket).
 */
public final class GauntExecute {
    public static final String ANIMATION = "execute";
    public static final int TICKS = 68;
    public static final int SNAP_TICK = 40;
    public static final int DROP_TICK = 52;
    public static final int COOLDOWN_TICKS = 600;
    /** The neck snap's crack is this far into its sound (tools/haunted_lord/sounds.py). */
    private static final int SNAP_SOUND_LEAD = 6;
    private static final double REACH = 3.5;
    /** The model's pixels in blocks (client/GauntRenderer draws it at 0.75 of the glTF's 1/16). */
    private static final double PX = 0.75 / 16.0;
    private static final SoundEvent NECK_SNAP = SoundEvent.createVariableRangeEvent(new ResourceLocation("mdvlcraft", "haunted_lord.neck_snap"));

    private static final class State {
        final LivingEntity victim;
        final float yaw;
        final long start;
        final boolean noAi;
        final boolean noGravity;
        boolean held = true;

        State(LivingEntity victim, float yaw, long start) {
            this.victim = victim;
            this.yaw = yaw;
            this.start = start;
            this.noAi = victim instanceof Mob mob && mob.isNoAi();
            this.noGravity = victim.isNoGravity();
        }
    }

    private static final Map<UUID, State> SERVER = new HashMap<>();

    private GauntExecute() {
    }

    // ---- the path (both sides) ----

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** anims.py grip(): where the right hand's knuckles are at `t` seconds, in the model's pixels (Blockbench space:
     * +x its right, +y up, -z its front). Only from the grab on (the victim waits at GRAB before it). */
    static Vec3 grip(float t) {
        Vec3 grab = new Vec3(6.0, 31.0, -19.0);
        Vec3 lift = new Vec3(5.0, 45.0, -17.0);
        float snap = SNAP_TICK / 20.0F;
        float drop = DROP_TICK / 20.0F;
        if (t < 0.6F) {
            return grab;
        }
        if (t < 1.5F) {
            return grab.lerp(lift, smooth((t - 0.6F) / 0.9F));
        }
        if (t < snap) {
            return lift.add(0.8 * Mth.sin(t * 37.0F), 0.8 * Mth.sin(t * 29.0F + 1.0F), 0.0);
        }
        if (t < drop) {
            return lift.add(0.0, -0.6 * smooth((t - snap) / 0.2F), 0.0);
        }
        return lift.add(0.0, -0.6 - 4.0 * smooth((t - drop) / 0.2F), 0.0);
    }

    /** Where the victim's feet go, `t` seconds in, for the Haunted Lord at `origin` facing `yaw`: its neck in the hand. */
    public static Vec3 victimAt(Vec3 origin, float yaw, float t, LivingEntity victim) {
        Vec3 hand = grip(t);
        double sin = Mth.sin(yaw * Mth.DEG_TO_RAD);
        double cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        Vec3 forward = new Vec3(-sin, 0.0, cos);
        Vec3 right = new Vec3(-cos, 0.0, -sin);
        Vec3 at = origin.add(right.scale(hand.x * PX)).add(0.0, hand.y * PX, 0.0).add(forward.scale(-hand.z * PX));
        return at.add(forward.scale(victim.getBbWidth() / 2.0)).subtract(0.0, victim.getEyeHeight() * 0.9, 0.0);
    }

    // ---- server ----

    public static boolean busy(ServerPlayer player) {
        return SERVER.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player) {
        if (busy(player) || GauntAttack.busy(player) || !GauntLeap.executeReady(player)) {
            return;
        }
        LivingEntity victim = target(player);
        if (victim == null) {
            player.displayClientMessage(Component.translatable("ability.mdvlcraft.gaunt_execute.none").withStyle(ChatFormatting.RED), true);
            return;
        }
        Vec3 to = victim.position().subtract(player.position());
        float yaw = (float)(Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, player.getXRot());
        victim.stopRiding();
        victim.ejectPassengers();
        State state = new State(victim, yaw, player.level().getGameTime());
        if (victim instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
        }
        victim.setNoGravity(true);
        SERVER.put(player.getUUID(), state);
        hold(player, state, 0);
        GauntLeap.executeUsed(player);
        BinderNetwork.sendToTrackingAndSelf(player, new GauntActionPacket(player.getId(), ANIMATION, 0, victim.getId(), yaw));
    }

    /** The living thing the player looks at within reach that can be executed, if any. */
    @Nullable
    private static LivingEntity target(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(REACH));
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
            entity -> entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator(), REACH * REACH);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity victim) || !player.hasLineOfSight(victim)) {
            return null;
        }
        boolean protectedPlayer = victim instanceof Player other && (other.isCreative() || other.isSpectator());
        boolean taken = SERVER.values().stream().anyMatch(state -> state.victim == victim);
        if (protectedPlayer || taken || victim.isInvulnerable() || victim.getType().is(Tags.EntityTypes.BOSSES) || victim.getBbHeight() > 2.2F
            || victim.getBbWidth() > 1.0F || victim.getHealth() > victim.getMaxHealth() * 0.5F) {
            return null;
        }
        return victim;
    }

    private static void hold(ServerPlayer player, State state, int ticks) {
        LivingEntity victim = state.victim;
        Vec3 at = victimAt(player.position(), state.yaw, ticks / 20.0F, victim);
        float facing = state.yaw + 180.0F;
        if (victim instanceof ServerPlayer held) {
            held.connection.teleport(at.x, at.y, at.z, facing, held.getXRot());
        } else {
            victim.moveTo(at.x, at.y, at.z, facing, victim.getXRot());
        }
        victim.setYHeadRot(facing);
        victim.setYBodyRot(facing);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.resetFallDistance();
    }

    static void tick(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null) {
            return;
        }
        int ticks = (int)(player.level().getGameTime() - state.start);
        player.setDeltaMovement(Vec3.ZERO);
        if (state.held) {
            LivingEntity victim = state.victim;
            if (!victim.isAlive() || victim.level() != player.level()) {
                let(player, state);
            } else if (ticks >= DROP_TICK) {
                victim.hurt(player.damageSources().playerAttack(player), victim.getMaxHealth() * 50.0F);
                let(player, state);
            } else {
                hold(player, state, ticks);
                if (ticks == SNAP_TICK - SNAP_SOUND_LEAD) {
                    player.level().playSound(null, victim.getX(), victim.getEyeY(), victim.getZ(), NECK_SNAP, SoundSource.PLAYERS, 1.6F, 1.0F);
                }
            }
        }
        if (ticks >= TICKS) {
            stop(player);
        }
    }

    /** The victim is let go of (dead, or the execution was cut short): its own AI and gravity come back. */
    private static void let(ServerPlayer player, State state) {
        if (!state.held) {
            return;
        }
        state.held = false;
        if (state.victim instanceof Mob mob) {
            mob.setNoAi(state.noAi);
        }
        state.victim.setNoGravity(state.noGravity);
        BinderNetwork.sendToTrackingAndSelf(player, new GauntActionPacket(player.getId(), ANIMATION, (int)(player.level().getGameTime() - state.start), -1,
            state.yaw));
    }

    /** Ends any execution (done, or the form ended); the victim, if still held, is let go alive. */
    static void stop(ServerPlayer player) {
        State state = SERVER.remove(player.getUUID());
        if (state != null) {
            let(player, state);
            BinderNetwork.sendToTrackingAndSelf(player, GauntActionPacket.none(player.getId()));
        }
    }

    static void sendTo(ServerPlayer viewer, ServerPlayer target) {
        State state = SERVER.get(target.getUUID());
        if (state != null) {
            BinderNetwork.sendTo(viewer, new GauntActionPacket(target.getId(), ANIMATION, (int)(target.level().getGameTime() - state.start),
                state.held ? state.victim.getId() : -1, state.yaw));
        }
    }
}
