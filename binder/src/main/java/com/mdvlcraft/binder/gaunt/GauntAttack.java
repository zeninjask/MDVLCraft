package com.mdvlcraft.binder.gaunt;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.GauntActionPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Haunted Lord's claws (attacking in Gaunt Vessel's form; the client sends each press, GauntAttackPacket): a
 * three-hit chain on the ground (right slash, left slash, two-handed slam) that runs on like Epic Fight's combos: a press
 * during the end of one attack is kept and starts the next as soon as it may (from `next`), and a press within COMBO_TICKS
 * of the last attack finishing carries on the chain, else it starts again. In the air it rakes down with both claws
 * (jump_attack); sprinting it rushes forward in a frenzy of four slashes (dash_attack). Each attack is an animation of the
 * Haunted Lord's (art/haunted_lord/source/anims.py) sent to everyone tracking it (GauntActionPacket); it hits what is in
 * front of it, within `reach` and `arc` degrees, at each of its `hits` (seconds into the animation).
 */
public final class GauntAttack {
    enum Kind {
        ATTACK1("attack1", 10, 15, 7.0F, 3.2, 130.0, 0.2, 0.30F),
        ATTACK2("attack2", 10, 15, 7.0F, 3.2, 130.0, 0.2, 0.30F),
        ATTACK3("attack3", 15, 20, 11.0F, 3.5, 150.0, 0.8, 0.48F),
        JUMP("jump_attack", 12, 16, 10.0F, 3.0, 220.0, 0.5, 0.38F),
        DASH("dash_attack", 19, 24, 4.5F, 3.0, 120.0, 0.1, 0.24F, 0.44F, 0.64F, 0.84F);

        final String animation;
        /** Ticks until the next attack may start (the animation's settle). */
        final int next;
        final int ticks;
        final float damage;
        final double reach;
        final double arc;
        final double knockback;
        final float[] hits;

        Kind(String animation, int next, int ticks, float damage, double reach, double arc, double knockback, float... hits) {
            this.animation = animation;
            this.next = next;
            this.ticks = ticks;
            this.damage = damage;
            this.reach = reach;
            this.arc = arc;
            this.knockback = knockback;
            this.hits = hits;
        }
    }

    private static final int COMBO_TICKS = 12;
    /** A press this close to the end of an attack's `next` is kept for when it comes. */
    private static final int BUFFER_TICKS = 8;
    private static final Kind[] CHAIN = {Kind.ATTACK1, Kind.ATTACK2, Kind.ATTACK3};
    private static final SoundEvent SCREECH = SoundEvent.createVariableRangeEvent(new ResourceLocation("mdvlcraft", "haunted_lord.sprint"));

    private static final class State {
        Kind kind;
        long start;
        int chain;
        boolean queued;
    }

    private static final Map<UUID, State> SERVER = new HashMap<>();

    private GauntAttack() {
    }

    /** Whether an attack is still playing (no leap or execution meanwhile). */
    public static boolean busy(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        return state != null && state.kind != null && player.level().getGameTime() - state.start < state.kind.next;
    }

    public static void press(ServerPlayer player) {
        if (!GauntForm.active(player) || GauntExecute.busy(player) || GauntLeap.held(player) || GauntLeap.blocking(player)) {
            return;
        }
        State state = SERVER.computeIfAbsent(player.getUUID(), id -> new State());
        long now = player.level().getGameTime();
        if ((state.kind == null || now - state.start >= state.kind.next) && player.onGround() && GauntExecute.tryFromBehind(player)) {
            state.queued = false;
            return;
        }
        if (state.kind != null && now - state.start < state.kind.next) {
            if (state.kind.next - (now - state.start) <= BUFFER_TICKS) {
                state.queued = true;
            }
            return;
        }
        begin(player, state, now);
    }

    private static void begin(ServerPlayer player, State state, long now) {
        Kind kind;
        if (!player.onGround() && !player.isInWater()) {
            kind = Kind.JUMP;
            state.chain = 0;
        } else if (player.isSprinting()) {
            kind = Kind.DASH;
            state.chain = 0;
        } else {
            boolean carries = state.kind != null && state.chain > 0 && now - state.start <= state.kind.ticks + COMBO_TICKS;
            int step = carries ? state.chain % CHAIN.length : 0;
            kind = CHAIN[step];
            state.chain = step + 1;
        }
        state.kind = kind;
        state.start = now;
        state.queued = false;
        if (kind == Kind.DASH) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
            player.setDeltaMovement(flat.scale(1.1).add(0.0, 0.1, 0.0));
            player.hurtMarked = true;
            player.level().playSound(null, player.blockPosition(), SCREECH, SoundSource.PLAYERS, 1.2F, 1.2F);
        } else if (kind == Kind.JUMP) {
            player.level().playSound(null, player.blockPosition(), SCREECH, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        BinderNetwork.sendToTrackingAndSelf(player, new GauntActionPacket(player.getId(), kind.animation, 0, -1, Float.NaN));
    }

    static void tick(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null || state.kind == null) {
            return;
        }
        long now = player.level().getGameTime();
        long ticks = now - state.start;
        Kind kind = state.kind;
        for (float hit : kind.hits) {
            if (Math.round(hit * 20.0F) == ticks) {
                hit(player, kind);
            }
        }
        if (kind == Kind.DASH && ticks < 17 && player.onGround()) {   // keeps rushing through the frenzy
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(flat.scale(0.45).add(0.0, motion.y, 0.0));
            player.hurtMarked = true;
        }
        if (state.queued && ticks >= kind.next) {
            begin(player, state, now);
        }
    }

    private static void hit(ServerPlayer player, Kind kind) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize();
        double cos = Math.cos(Math.toRadians(kind.arc / 2.0));
        boolean struck = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(kind.reach))) {
            if (target == player || !target.isAlive() || player.isAlliedTo(target) || target instanceof Player other && (other.isSpectator() || other.isCreative())) {
                continue;
            }
            Vec3 to = target.position().subtract(player.position());
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double distance = flat.length() - target.getBbWidth() / 2.0;
            if (distance > kind.reach || Math.abs(to.y) > kind.reach || (flat.lengthSqr() > 0.04 && flat.normalize().dot(forward) < cos)) {
                continue;
            }
            if (target.hurt(player.damageSources().playerAttack(player), kind.damage)) {
                struck = true;
                if (kind.knockback > 0.0) {
                    target.knockback(kind.knockback, -forward.x, -forward.z);
                }
            }
        }
        Vec3 at = player.position().add(forward.scale(1.5)).add(0.0, player.getBbHeight() * 0.5, 0.0);
        player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            struck ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F,
            0.6F + 0.2F * player.getRandom().nextFloat());
        if (kind == Kind.ATTACK3 || kind == Kind.JUMP) {
            player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL, at.x, player.getY() + 0.1, at.z, 8, 0.6, 0.05, 0.6, 0.02);
        }
    }

    /** The form ended. */
    static void clear(ServerPlayer player) {
        SERVER.remove(player.getUUID());
    }
}
