package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.compat.wom.WomBlackout;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * The berserker's arts.
 * - Sparking Bloodlust: the Feral Claw's Beast Roar, then for BLOODLUST_TICKS more attack speed and a one-in-five chance
 *   that each strike is a Black Flash (MonkArts.blackFlash).
 * - Bull Rush: a shoulder charge (Epic Fight's dash) DASH_TICKS long; the first creature it meets is hit for
 *   RUSH_DAMAGE x attack damage and blacks out like a Punishment Kick (WomBlackout).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BerserkerArts {
    private static final String BEAST_ROAR = "efn:biped/nf_claw/nf_claw_beastroar";
    private static final UUID BLOODLUST_SPEED = UUID.fromString("0c5d6a51-3b77-4f6e-a0f3-2a9b4f1d7c11");
    private static final int BLOODLUST_TICKS = 300;
    private static final double BLOODLUST_ATTACK_SPEED = 0.3;
    private static final float BLACK_FLASH_CHANCE = 0.2F;
    private static final int DASH_TICKS = 8;
    private static final double DASH_SPEED = 1.1;
    private static final float RUSH_DAMAGE = 1.5F;
    /** Bloodlust: when it ends (game time). */
    private static final Map<UUID, Long> BLOODLUST = new HashMap<>();
    /** Bull Rush: the dash direction and the ticks left. */
    private static final Map<UUID, Rush> RUSHES = new HashMap<>();

    private BerserkerArts() {
    }

    static boolean sparkingBloodlust(ServerPlayer player) {
        patch(player).playAnimationSynchronized(AnimationManager.byKey(BEAST_ROAR), 0.0F);
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        speed.removeModifier(BLOODLUST_SPEED);
        speed.addTransientModifier(new AttributeModifier(BLOODLUST_SPEED, "Sparking Bloodlust", BLOODLUST_ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_BASE));
        BLOODLUST.put(player.getUUID(), player.level().getGameTime() + BLOODLUST_TICKS);
        return true;
    }

    static boolean bullRush(ServerPlayer player) {
        patch(player).playAnimationSynchronized(Animations.FIST_DASH, 0.0F);
        Vec3 look = player.getLookAngle();
        RUSHES.put(player.getUUID(), new Rush(new Vec3(look.x, 0.0, look.z).normalize(), DASH_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }

    private static ServerPlayerPatch patch(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }
        return patch;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && BLOODLUST.containsKey(player.getUUID())
            && MonkArts.isStrike(event.getSource(), player) && player.getRandom().nextFloat() < BLACK_FLASH_CHANCE) {
            MonkArts.blackFlash(player, event.getEntity(), event);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Long until = BLOODLUST.get(player.getUUID());
        if (until != null && player.level().getGameTime() >= until) {
            end(player);
        }
        Rush rush = RUSHES.get(player.getUUID());
        if (rush != null) {
            rush(player, rush);
        }
    }

    private static void rush(ServerPlayer player, Rush rush) {
        player.setDeltaMovement(rush.direction.x * DASH_SPEED, player.getDeltaMovement().y, rush.direction.z * DASH_SPEED);
        player.hurtMarked = true;
        LivingEntity hit = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8),
            entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player)).stream().findFirst().orElse(null);
        if (hit != null) {
            hit.invulnerableTime = 0;
            hit.hurt(player.damageSources().playerAttack(player), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * RUSH_DAMAGE);
            WomBlackout.strike(player, hit);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            RUSHES.remove(player.getUUID());
        } else if (--rush.ticksLeft <= 0) {
            RUSHES.remove(player.getUUID());
        }
    }

    private static void end(ServerPlayer player) {
        BLOODLUST.remove(player.getUUID());
        player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(BLOODLUST_SPEED);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
            RUSHES.remove(player.getUUID());
        }
    }

    private static final class Rush {
        final Vec3 direction;
        int ticksLeft;

        Rush(Vec3 direction, int ticksLeft) {
            this.direction = direction;
            this.ticksLeft = ticksLeft;
        }
    }
}
