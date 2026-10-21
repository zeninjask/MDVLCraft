package com.mdvlcraft.binder.gaunt;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.GauntStatePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mdvlcraft.binder.network.GauntActionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Haunted Lord's leap (gaunt/GauntForm, ability/GauntAbility.LEAP): held, it charges for up to CHARGE_TICKS (the
 * player is held still, and client/LeapTrajectory draws the path); released, it throws the player along where they look,
 * faster the longer it was held. Mid-leap, touching a wall or a ceiling makes them cling to it (the client sees the touch
 * and asks, GauntClingPacket; the server checks the block is there): no gravity, held in place, drawn crawling on the
 * surface (client/GauntRenderer) until they jump off, leap again, or the block goes. The state goes to the player and
 * everyone tracking them (GauntStatePacket).
 *
 * Horrify (ability/GauntAbility.HORRIFY) lives here too, sharing that state: the Haunted Lord screeches (the cast
 * animation) and every other player within HORRIFY_RANGE blocks is blinded and slowed. So do the dodge (a hop with
 * DODGE_IFRAMES of untouchability), the guard (blows from the front deal BLOCKED_DAMAGE of their damage) and the parry (a
 * blow within PARRY_TICKS of raising the guard is turned aside and the attacker staggered), and crawling onto walls and
 * ceilings (sneaking into them: the cling, moved along the surface by the client, client/GauntClient).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GauntLeap {
    public static final int CHARGE_TICKS = 20;
    public static final int COOLDOWN_TICKS = 30;
    public static final int HORRIFY_COOLDOWN_TICKS = 400;
    private static final double HORRIFY_RANGE = 20.0;
    private static final int HORRIFY_TICKS = 100;
    private static final SoundEvent SCREECH = SoundEvent.createVariableRangeEvent(new ResourceLocation("mdvlcraft", "haunted_lord.sprint"));
    private static final SoundEvent HORRIFY = SoundEvent.createVariableRangeEvent(new ResourceLocation("mdvlcraft", "haunted_lord.horrify"));

    private static final class State {
        long chargingSince = -1;
        @Nullable
        Direction cling;
        boolean leaping;
        long readyAt;
        long horrifyReadyAt;
        long executeReadyAt;
        long blockSince = -1;
        long dodgeUntil;
        long dodgeReadyAt;
    }

    public static final int DODGE_COOLDOWN = 20;
    private static final int DODGE_IFRAMES = 8;
    /** A blow landing this soon after the block went up is parried. */
    private static final int PARRY_TICKS = 6;
    private static final float BLOCKED_DAMAGE = 0.3F;
    /** Client side only (written by client/GauntClient): the players this client sees clinging, by entity id. */
    public static final java.util.Set<Integer> CLIENT_CLING = new java.util.HashSet<>();

    private static final Map<UUID, State> SERVER = new HashMap<>();

    private GauntLeap() {
    }

    /** Launch speed (blocks per tick) for a charge of 0..1. */
    public static double speed(float charge) {
        return 0.8 + 1.6 * charge;
    }

    public static float charge(long heldTicks) {
        return Math.min(1.0F, Math.max(0.0F, heldTicks / (float)CHARGE_TICKS));
    }

    /** One tick of a player's flight through the air: vanilla's air drag and gravity, applied after the move. */
    public static Vec3 step(Vec3 velocity) {
        return new Vec3(velocity.x * 0.91, (velocity.y - 0.08) * 0.98, velocity.z * 0.91);
    }

    /** Whether a block the player can stand against is just beyond their hitbox on that side. */
    public static boolean against(Player player, Direction face) {
        AABB box = player.getBoundingBox().move(face.getStepX() * 0.1, face.getStepY() * 0.1, face.getStepZ() * 0.1);
        return !player.level().noCollision(player, box);
    }

    private static State state(ServerPlayer player) {
        return SERVER.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void press(ServerPlayer player) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (now < state.readyAt || state.chargingSince >= 0 || GauntExecute.busy(player) || GauntAttack.busy(player)) {
            return;
        }
        state.chargingSince = now;
        sync(player, state);
    }

    public static void release(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null || state.chargingSince < 0) {
            return;
        }
        long now = player.level().getGameTime();
        float charge = charge(now - state.chargingSince);
        state.chargingSince = -1;
        state.readyAt = now + COOLDOWN_TICKS;
        letGo(player, state);
        state.leaping = true;
        player.setDeltaMovement(player.getLookAngle().scale(speed(charge)));
        player.hurtMarked = true;
        player.resetFallDistance();
        player.level().playSound(null, player.blockPosition(), SCREECH, SoundSource.PLAYERS, 1.2F, 1.1F);
        sync(player, state);
    }

    /** Horrify: blinds and slows every other player within range (not spectators or creative players). */
    public static void horrify(ServerPlayer player) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (now < state.horrifyReadyAt || GauntExecute.busy(player) || GauntAttack.busy(player)) {
            return;
        }
        state.horrifyReadyAt = now + HORRIFY_COOLDOWN_TICKS;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && !other.isSpectator() && !other.isCreative() && other.distanceToSqr(player) <= HORRIFY_RANGE * HORRIFY_RANGE) {
                other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, HORRIFY_TICKS, 0), player);
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HORRIFY_TICKS, 1), player);
            }
        }
        player.level().playSound(null, player.blockPosition(), HORRIFY, SoundSource.PLAYERS, 2.5F, 1.0F);
        GauntForm.playCast(player);
        sync(player, state);
    }

    /** Whether the player is charging a leap or clinging (no attacking meanwhile). */
    static boolean held(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        return state != null && (state.chargingSince >= 0 || state.cling != null);
    }

    static boolean executeReady(ServerPlayer player) {
        State state = state(player);
        return player.level().getGameTime() >= state.executeReadyAt && !held(player);
    }

    static void executeUsed(ServerPlayer player) {
        State state = state(player);
        state.executeReadyAt = player.level().getGameTime() + GauntExecute.COOLDOWN_TICKS;
        sync(player, state);
    }

    /** The client says the player touched `face` mid-leap (cling), or jumped off (null). */
    public static void cling(ServerPlayer player, @Nullable Direction face) {
        if (!GauntForm.active(player)) {
            return;
        }
        State state = state(player);
        if (face == null) {
            letGo(player, state);
        } else if ((state.leaping || state.cling != null || player.isShiftKeyDown()) && face != Direction.DOWN && against(player, face)) {
            // from a leap, crawling (sneaking) into a wall, or moving on from one surface to the next
            boolean fresh = state.cling == null;
            state.cling = face;
            state.leaping = false;
            state.blockSince = -1;
            player.setNoGravity(true);
            if (fresh) {
                player.setDeltaMovement(Vec3.ZERO);
            }
            player.resetFallDistance();
            player.refreshDimensions();
        }
        sync(player, state);   // also answers a refused touch, so the client stops predicting the cling
    }

    private static void letGo(ServerPlayer player, State state) {
        if (state.cling != null) {
            state.cling = null;
            player.setNoGravity(false);
            player.refreshDimensions();
        }
    }

    /** Whether the player is clinging to a surface (both sides). */
    public static boolean clinging(Player player) {
        if (player.level().isClientSide()) {
            return CLIENT_CLING.contains(player.getId());
        }
        State state = SERVER.get(player.getUUID());
        return state != null && state.cling != null;
    }

    /** Dodge: a quick hop in the direction the player is steering (backwards when they are not), briefly untouchable. */
    public static void dodge(ServerPlayer player, float strafe, float forward) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (!GauntForm.active(player) || now < state.dodgeReadyAt || state.cling != null || state.chargingSince >= 0 || !player.onGround()
            || GauntExecute.busy(player) || GauntAttack.busy(player)) {
            return;
        }
        if (Math.abs(strafe) < 0.1F && Math.abs(forward) < 0.1F) {
            forward = -1.0F;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 ahead = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 left = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        Vec3 way = ahead.scale(forward).add(left.scale(strafe)).normalize();
        player.setDeltaMovement(way.scale(1.1).add(0.0, 0.3, 0.0));
        player.hurtMarked = true;
        state.blockSince = -1;
        state.dodgeUntil = now + DODGE_IFRAMES;
        state.dodgeReadyAt = now + DODGE_COOLDOWN;
        player.level().playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 0.6F);
        BinderNetwork.sendToTrackingAndSelf(player, new GauntActionPacket(player.getId(), "dodge", 0, -1, Float.NaN));
        sync(player, state);
    }

    /** Raising (on) or lowering the guard (held: the guard key). */
    public static void block(ServerPlayer player, boolean on) {
        State state = state(player);
        long now = player.level().getGameTime();
        boolean can = GauntForm.active(player) && state.cling == null && state.chargingSince < 0 && !GauntExecute.busy(player);
        long since = on && can ? (state.blockSince >= 0 ? state.blockSince : now) : -1;
        if (since != state.blockSince) {
            state.blockSince = since;
            sync(player, state);
        }
    }

    static boolean blocking(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        return state != null && state.blockSince >= 0;
    }

    /** Dodging: untouchable. Guarding against a blow from the front: parried if the guard has only just gone up (no
     * damage, the attacker is thrown back and staggered), else blocked (BLOCKED_DAMAGE of it gets through). */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = SERVER.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < state.dodgeUntil) {
            event.setCanceled(true);
            return;
        }
        if (state.blockSince >= 0 && fromFront(player, event.getSource()) && now - state.blockSince <= PARRY_TICKS) {
            event.setCanceled(true);
            state.blockSince = now - PARRY_TICKS - 1;      // one parry per raise
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                Vec3 away = attacker.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
                attacker.knockback(1.2, -away.x, -away.z);
                var patch = EpicFightCapabilities.getEntityPatch(attacker, LivingEntityPatch.class);
                if (patch != null) {
                    patch.applyStun(StunType.LONG, 1.0F);
                }
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.6F);
            player.level().playSound(null, player.blockPosition(), SCREECH, SoundSource.PLAYERS, 0.8F, 1.3F);
            BinderNetwork.sendToTrackingAndSelf(player, new GauntActionPacket(player.getId(), "parry", 0, -1, Float.NaN));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && blocking(player) && fromFront(player, event.getSource())) {
            event.setAmount(event.getAmount() * BLOCKED_DAMAGE);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    private static boolean fromFront(ServerPlayer player, DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        Vec3 to = from.subtract(player.position()).multiply(1.0, 0.0, 1.0);
        return to.lengthSqr() < 1.0E-4 || to.normalize().dot(player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize()) > 0.0;
    }

    static void tick(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null) {
            return;
        }
        boolean changed = false;
        if (state.leaping && player.onGround()) {
            state.leaping = false;
            changed = true;
        }
        if (state.cling != null && !against(player, state.cling)) {
            letGo(player, state);
            changed = true;
        }
        if (changed) {
            sync(player, state);
        }
    }

    /** The form ended: no charge, no cling. */
    static void clear(ServerPlayer player) {
        State state = SERVER.remove(player.getUUID());
        if (state != null) {
            letGo(player, state);
            BinderNetwork.sendToTrackingAndSelf(player, new GauntStatePacket(player.getId(), -1, -1, false, 0, 0, 0, false));
        }
    }

    static void sendTo(ServerPlayer viewer, ServerPlayer target) {
        State state = SERVER.get(target.getUUID());
        if (state != null) {
            BinderNetwork.sendTo(viewer, packet(target, state));
        }
    }

    private static void sync(ServerPlayer player, State state) {
        BinderNetwork.sendToTrackingAndSelf(player, packet(player, state));
    }

    private static GauntStatePacket packet(ServerPlayer player, State state) {
        long now = player.level().getGameTime();
        return new GauntStatePacket(player.getId(), state.chargingSince < 0 ? -1 : (int)(now - state.chargingSince),
            state.cling == null ? -1 : state.cling.get3DDataValue(), state.leaping, (int)Math.max(0, state.readyAt - now),
            (int)Math.max(0, state.horrifyReadyAt - now), (int)Math.max(0, state.executeReadyAt - now), state.blockSince >= 0);
    }
}
