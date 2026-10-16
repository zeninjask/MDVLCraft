package com.mdvlcraft.binder.seraph;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SeraphFormPacket;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * Seraph Embodiment (modified/SeraphEmbodiment): the player becomes the Seraphim (client/SeraphRenderer draws it in their
 * place). The form flies in (the model's spawn animation), lasts until it is cast again or its recast runs out, and
 * leaves (spawn reversed). Throughout: slow flight (up, down and sideways, no sprinting), a hitbox of only the eye, no
 * attacks or item use, no Epic Fight skills or other abilities (Iron's spells only, Modified abilities excepted), 20%
 * more spell power, 50% more mana regeneration, Regeneration I and no fall damage.
 *
 * The server keeps each player's phase and sends it to them and everyone tracking them (SeraphFormPacket); clients keep
 * what they were told in CLIENT, so in() answers on both sides.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SeraphForm {
    public static final String SPELL = "mdvlcraft:seraph_embodiment";
    /** The model's scale: about 20 blocks across the wings. */
    public static final float SCALE = 0.8F;
    /** The hitbox: the eye (20 model pixels) at SCALE. */
    public static final float EYE = 1.25F * SCALE;
    /** Entering and leaving play the 4.5 s spawn animation in this many ticks (1.5 times as fast). */
    public static final int SPAWN_TICKS = 60;
    private static final float FLY_SPEED = 0.02F;
    private static final UUID SPELL_POWER = UUID.fromString("3e9b1c52-7a4d-4f1e-9c2b-5d8a6f0e1b11");
    private static final UUID MANA_REGEN = UUID.fromString("3e9b1c52-7a4d-4f1e-9c2b-5d8a6f0e1b12");
    private static final UUID SKILL_LISTENER = UUID.fromString("3e9b1c52-7a4d-4f1e-9c2b-5d8a6f0e1b13");
    private static final UUID ATTACK_LISTENER = UUID.fromString("3e9b1c52-7a4d-4f1e-9c2b-5d8a6f0e1b14");

    public enum FormPhase {
        NONE,
        ENTERING,
        ACTIVE,
        EXITING
    }

    private static final class State {
        FormPhase phase = FormPhase.ENTERING;
        int ticks;
        final boolean epicFightMode;
        final float flyingSpeed;

        State(boolean epicFightMode, float flyingSpeed) {
            this.epicFightMode = epicFightMode;
            this.flyingSpeed = flyingSpeed;
        }
    }

    /** A client's view of a player's form: the phase and the level time it began, and when they last began a spell. */
    public static final class Seen {
        public final FormPhase phase;
        public final long since;
        public long castAt = Long.MIN_VALUE / 2;

        public Seen(FormPhase phase, long since) {
            this.phase = phase;
            this.since = since;
        }
    }

    private static final Map<UUID, State> SERVER = new HashMap<>();
    /** Client side only (written by SeraphFormPacket): the players this client sees in the form, by entity id. */
    public static final Map<Integer, Seen> CLIENT = new HashMap<>();

    private SeraphForm() {
    }

    public static boolean in(Player player) {
        return player.level().isClientSide() ? CLIENT.containsKey(player.getId()) : SERVER.containsKey(player.getUUID());
    }

    public static FormPhase phase(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        return state == null ? FormPhase.NONE : state.phase;
    }

    /** Whether a spell may be cast in the form: Iron's spells, not the Modified abilities (Seraph Embodiment itself excepted). */
    public static boolean castable(String spellId) {
        return !spellId.startsWith("mdvlcraft:") || spellId.equals(SPELL);
    }

    public static void enter(ServerPlayer player) {
        if (SERVER.containsKey(player.getUUID())) {
            return;
        }
        ServerPlayerPatch patch = patch(player);
        State state = new State(patch.isEpicFightMode(), player.getAbilities().getFlyingSpeed());
        SERVER.put(player.getUUID(), state);
        patch.toVanillaMode(true);
        player.stopUsingItem();
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(FLY_SPEED);
        player.onUpdateAbilities();
        modifier(player, AttributeRegistry.SPELL_POWER.get(), SPELL_POWER, "Seraph Embodiment", 0.2);
        modifier(player, AttributeRegistry.MANA_REGEN.get(), MANA_REGEN, "Seraph Embodiment", 0.5);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        player.refreshDimensions();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.7F);
        sync(player, state, false);
    }

    /** Starts leaving the form (the reversed spawn animation); the form ends when it has played. */
    public static void leave(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null || state.phase == FormPhase.EXITING) {
            return;
        }
        state.phase = FormPhase.EXITING;
        state.ticks = 0;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 0.7F);
        sync(player, state, false);
    }

    /** Ends the form at once: the player's own body, flight, hitbox, attributes and Epic Fight mode come back. */
    private static void end(ServerPlayer player) {
        State state = SERVER.remove(player.getUUID());
        if (state == null) {
            return;
        }
        player.getAbilities().setFlyingSpeed(state.flyingSpeed);
        player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
        player.onUpdateAbilities();
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
        removeModifier(player, AttributeRegistry.SPELL_POWER.get(), SPELL_POWER);
        removeModifier(player, AttributeRegistry.MANA_REGEN.get(), MANA_REGEN);
        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        if (regeneration != null && regeneration.isInfiniteDuration()) {
            player.removeEffect(MobEffects.REGENERATION);
        }
        if (state.epicFightMode) {
            patch(player).toEpicFightMode(true);
        }
        player.refreshDimensions();
        BinderNetwork.sendToTrackingAndSelf(player, new SeraphFormPacket(player.getId(), FormPhase.NONE, 0, false));
    }

    private static void sync(ServerPlayer player, State state, boolean cast) {
        BinderNetwork.sendToTrackingAndSelf(player, new SeraphFormPacket(player.getId(), state.phase, state.ticks, cast));
    }

    private static ServerPlayerPatch patch(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }
        return patch;
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, Operation.MULTIPLY_BASE));
        }
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent event) {
        if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = SERVER.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.ticks++;
        if (state.phase == FormPhase.ENTERING && state.ticks >= SPAWN_TICKS) {
            state.phase = FormPhase.ACTIVE;
            state.ticks = 0;
            sync(player, state, false);
        } else if (state.phase == FormPhase.EXITING && state.ticks >= SPAWN_TICKS) {
            end(player);
            return;
        }
        player.setSprinting(false);
        if (!player.getAbilities().flying || !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    /** The hitbox is the eye alone, centred on the eye (client/SeraphRenderer puts the model's eye there). */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && player.level() != null && in(player)) {
            event.setNewSize(EntityDimensions.fixed(EYE, EYE), false);
            event.setNewEyeHeight(EYE / 2.0F);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            patch(player).getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, SKILL_LISTENER, cast -> {
                if (in(player)) {
                    cast.setSkillExecutable(false);
                }
            });
            patch(player).getEventListener().addEventListener(EventType.BASIC_ATTACK_EVENT, ATTACK_LISTENER, attack -> {
                if (in(player)) {
                    attack.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            State state = SERVER.get(target.getUUID());
            if (state != null) {
                BinderNetwork.sendTo(viewer, new SeraphFormPacket(target.getId(), state.phase, state.ticks, false));
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && in(player)) {
            event.setCanceled(true);
        }
    }

    /** Spells only: Iron's spells, not the Modified abilities. */
    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (in(event.getEntity()) && !castable(event.getSpellId())) {
            event.setCanceled(true);
            refuse(event.getEntity());
        }
    }

    /** A spell begun in the form plays the pose animation. */
    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getSpellId().equals(SPELL)) {
            State state = SERVER.get(player.getUUID());
            if (state != null) {
                sync(player, state, true);
            }
        }
    }

    public static void refuse(Player player) {
        player.displayClientMessage(Component.translatable("ability.mdvlcraft.seraph.spells_only").withStyle(ChatFormatting.RED), true);
    }

    // No attacks and no item use (both sides, so the client does not swing or predict).
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        block(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) {
            block(event, player);
        }
    }

    private static void block(Event event, Player player) {
        if (in(player)) {
            event.setCanceled(true);
        }
    }
}
