package com.mdvlcraft.binder.ability;

import cursedfate.CursedfateMod;
import cursedfate.procedures.SetupAnimationsProcedure.CursedfateModAnimationMessage;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * While the cast key is held on a standing Malevolent Shrine (closed or open), the caster holds the shrine's hand sign
 * (Cursed Fate's shrineanimation) and does nothing else: rooted in place, no jumping, attacks, item use, block breaking,
 * Iron's spells, Epic Fight skills or other abilities (AbilityActions.press). Releasing the key ends it.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ShrineSign {
    private static final UUID ROOT = UUID.fromString("5d0c8f43-2b7e-4a61-9e3d-7f1a6c2b8e94");
    private static final UUID SKILL_LISTENER = UUID.fromString("b3e9a1d7-6c42-4f85-8a0e-1d5f7c3b9e26");
    private static final Set<UUID> SIGNING = new HashSet<>();

    private ShrineSign() {
    }

    public static boolean signing(ServerPlayer player) {
        return SIGNING.contains(player.getUUID());
    }

    static void start(ServerPlayer player) {
        if (SIGNING.add(player.getUUID())) {
            animate(player, "shrineanimation");
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(ROOT) == null) {
                speed.addTransientModifier(new AttributeModifier(ROOT, "Shrine hand sign", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    static void stop(ServerPlayer player) {
        if (SIGNING.remove(player.getUUID())) {
            animate(player, "emptyanimation");
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(ROOT);
            }
        }
    }

    /** Plays one of Cursed Fate's player animations (by its name) on the player, for them and everyone watching. */
    static void animate(ServerPlayer player, String animation) {
        CursedfateMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
            new CursedfateModAnimationMessage(Component.literal(animation), player.getId(), true));
    }

    /** Cursed Fate damage: one of its damage types, or a hit by one of its entities (slashes, webs, flames). */
    private static boolean cursedFate(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return source.typeHolder().unwrapKey().map(k -> k.location().getNamespace().equals("cursedfate")).orElse(false)
            || direct != null && "cursedfate".equals(ForgeRegistries.ENTITY_TYPES.getKey(direct.getType()).getNamespace());
    }

    private static boolean signing(Object entity) {
        return entity instanceof ServerPlayer player && signing(player);
    }

    private static void cancel(Event event) {
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        if (signing(event.getEntity())) {
            event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().multiply(0.0, 0.0, 0.0));
            event.getEntity().hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (signing(event.getEntity())) {
            cancel(event);
        }
    }

    /** Any hit the caster deals (Epic Fight swings included) other than the shrine's own cuts. */
    @SubscribeEvent
    public static void onHit(LivingAttackEvent event) {
        if (signing(event.getSource().getEntity()) && !cursedFate(event.getSource())) {
            cancel(event);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent event) {
        if (signing(event.getEntity())) {
            cancel(event);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (signing(event.getPlayer())) {
            cancel(event);
        }
    }

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (signing(event.getEntity())) {
            cancel(event);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch != null) {
                patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, SKILL_LISTENER, cast -> {
                    if (signing(player)) {
                        cast.setSkillExecutable(false);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
        }
    }
}
