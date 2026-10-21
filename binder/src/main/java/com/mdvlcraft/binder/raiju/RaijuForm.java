package com.mdvlcraft.binder.raiju;

import com.mdvlcraft.binder.modified.StrikeSpells;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.RaijuFormPacket;
import com.somakespells.entity.LightningFieldEntity;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * Yakusanoikazuchi (modified/Yakusanoikazuchi): the player takes the thunder god's shape (client/RaijuClient draws them
 * with its skin and six darkened Vatansever swords circling them, each in its own charged swirl) until they cast it again
 * (the form's wheel holds only Transform Back, ability/GauntAbility.RAIJU_WHEEL). In the form: no other spells, abilities
 * or Epic Fight skills; the dodge key does Thunder Step (modified/StrikeSpells.ThunderStep) to the locked-on enemy (where
 * the player looks without one), every DODGE_COOLDOWN ticks at most; a melee hit has a 1 in 6 chance to drop a short
 * Lightning Field (FIELD_TICKS) on the target that stuns whatever it shocks (combat/LightningFieldKnockback).
 *
 * The server keeps who is in the form and sends it to them and everyone tracking them (RaijuFormPacket); clients keep it
 * in CLIENT, so in() answers on both sides.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RaijuForm {
    public static final String SPELL = "mdvlcraft:yakusanoikazuchi";
    public static final int DODGE_COOLDOWN = 40;
    public static final int FIELD_TICKS = 40;
    /** Lightning Fields dropped by the form's hits carry this tag: they stun. */
    public static final String STUN_FIELD = "mdvlcraft_raiju_field";
    private static final UUID SKILL_LISTENER = UUID.fromString("5a3e1c9d-7b42-4f6e-9c1a-8d2b7e4f0a31");
    private static final double STEP_RANGE = 14.0;

    /** Server side: each player in the form and the game time their dodge is ready again. */
    private static final Map<UUID, Long> SERVER = new HashMap<>();
    /** Client side only (written by RaijuFormPacket): the players this client sees in the form, by entity id. */
    public static final Set<Integer> CLIENT = new HashSet<>();

    private RaijuForm() {
    }

    public static boolean in(Player player) {
        return player.level().isClientSide() ? CLIENT.contains(player.getId()) : SERVER.containsKey(player.getUUID());
    }

    public static void enter(ServerPlayer player) {
        if (SERVER.putIfAbsent(player.getUUID(), 0L) == null) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.4F);
            BinderNetwork.sendToTrackingAndSelf(player, new RaijuFormPacket(player.getId(), true));
        }
    }

    public static void leave(ServerPlayer player) {
        if (SERVER.remove(player.getUUID()) != null) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
            BinderNetwork.sendToTrackingAndSelf(player, new RaijuFormPacket(player.getId(), false));
        }
    }

    /** The dodge: Thunder Step to the locked-on enemy, or where the player looks. */
    private static void step(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long ready = SERVER.get(player.getUUID());
        AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation("mdvlcraft:thunder_step"));
        if (ready == null || now < ready || !(spell instanceof StrikeSpells.ThunderStep thunderStep)) {
            return;
        }
        SERVER.put(player.getUUID(), now + DODGE_COOLDOWN);
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        LivingEntity target = patch != null ? patch.getTarget() : null;
        Vec3 dest;
        if (target != null && target.isAlive() && target.distanceTo(player) <= STEP_RANGE * 2.0) {
            // just short of the target, on the side the player comes from
            Vec3 toward = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            double gap = target.getBbWidth() / 2.0 + 0.8;
            dest = toward.lengthSqr() > gap * gap ? target.position().subtract(toward.normalize().scale(gap)) : player.position();
        } else {
            Vec3 eye = player.getEyePosition();
            Vec3 hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(STEP_RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
            dest = hit.subtract(player.getLookAngle().scale(0.6));
        }
        dest = Utils.moveToRelativeGroundLevel(player.level(), dest, 3);
        thunderStep.step(player.serverLevel(), player, dest, thunderStep.getSpellPower(1, player));
    }

    /** A melee hit by a player in the form: 1 in 6 drops a short Lightning Field on the target that stuns. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() == player && in(player)
            && player.getRandom().nextInt(6) == 0) {
            LivingEntity target = event.getEntity();
            LightningFieldEntity field = new LightningFieldEntity(player.level(), player, 4.0F, FIELD_TICKS, 3.0F);
            field.setPos(target.getX(), target.getY(), target.getZ());
            field.getPersistentData().putBoolean(STUN_FIELD, true);
            player.level().addFreshEntity(field);
        }
    }

    /** What a stunning field (STUN_FIELD) does to what it shocks. */
    public static void stun(LivingEntity target) {
        var patch = EpicFightCapabilities.getEntityPatch(target, yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        if (patch != null) {
            patch.applyStun(StunType.SHORT, 0.6F);
        }
    }

    /** No Epic Fight skills in the form; the dodge becomes the Thunder Step. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch != null) {
                patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, SKILL_LISTENER, cast -> {
                    if (in(player)) {
                        cast.setSkillExecutable(false);
                        if (cast.getSkillContainer().getSlot() == SkillSlots.DODGE) {
                            step(player);
                        }
                    }
                });
            }
        }
    }

    /** No spells but Yakusanoikazuchi itself. */
    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (in(event.getEntity()) && !event.getSpellId().equals(SPELL)) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("ability.mdvlcraft.raiju.locked").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer && SERVER.containsKey(target.getUUID())) {
            BinderNetwork.sendTo(viewer, new RaijuFormPacket(target.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SERVER.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }
}
