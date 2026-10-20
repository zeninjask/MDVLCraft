package com.mdvlcraft.binder.gaunt;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.GauntFormPacket;
import com.mdvlcraft.binder.seraph.SeraphForm.FormPhase;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * Gaunt Vessel (modified/GauntVessel): the player becomes the Haunted Lord (client/GauntRenderer draws it in their place).
 * Entering, the player is lifted and their bones snap one by one (SNAPS) until the body tears into the
 * Haunted Lord, which unfolds (its transform animation); leaving plays all of it backwards at twice the speed. While
 * changing the player cannot move or jump. In the form: 25% faster, 20% more spirit spell power, Night Vision; spells
 * only (Iron's spells, not the Modified abilities), no attacks, item use or Epic Fight skills.
 *
 * The server keeps each player's phase and sends it to them and everyone tracking them (GauntFormPacket); clients keep
 * what they were told in CLIENT, so in() answers on both sides.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GauntForm {
    public static final String SPELL = "mdvlcraft:gaunt_vessel";
    /** The player's own body snapping, then the Haunted Lord's transform animation (1.8 s). */
    public static final int SNAP_TICKS = 64;
    public static final int EMERGE_TICKS = 36;
    public static final int ENTER_TICKS = SNAP_TICKS + EMERGE_TICKS;
    /** Leaving plays the entering backwards, twice as fast. */
    public static final int EXIT_TICKS = ENTER_TICKS / 2;
    /** The ticks of entering at which a bone snaps (client/GauntRenderer poses the body to match). */
    public static final int[] SNAPS = {12, 19, 26, 32, 37, 43, 47, 51, 55, 58, 61};
    private static final ResourceLocation SPIRIT_POWER = new ResourceLocation("iss_magicfromtheeast", "spirit_spell_power");
    private static final UUID SPEED = UUID.fromString("7c2f4a91-3d6e-4b8a-a1f5-2e9d0c6b7a21");
    private static final UUID SPELL_POWER = UUID.fromString("7c2f4a91-3d6e-4b8a-a1f5-2e9d0c6b7a22");
    private static final UUID FROZEN = UUID.fromString("7c2f4a91-3d6e-4b8a-a1f5-2e9d0c6b7a23");
    private static final UUID SKILL_LISTENER = UUID.fromString("7c2f4a91-3d6e-4b8a-a1f5-2e9d0c6b7a24");
    private static final UUID ATTACK_LISTENER = UUID.fromString("7c2f4a91-3d6e-4b8a-a1f5-2e9d0c6b7a25");

    private static final class State {
        FormPhase phase = FormPhase.ENTERING;
        int ticks;
        final boolean epicFightMode;

        State(boolean epicFightMode) {
            this.epicFightMode = epicFightMode;
        }
    }

    private static final Map<UUID, State> SERVER = new HashMap<>();
    /** Client side only (written by GauntFormPacket): the players this client sees in the form, by entity id. */
    public static final Map<Integer, Seen> CLIENT = new HashMap<>();

    private GauntForm() {
    }

    public static boolean in(Player player) {
        return player.level().isClientSide() ? CLIENT.containsKey(player.getId()) : SERVER.containsKey(player.getUUID());
    }

    /** Where in the entering sequence (ticks) a phase `ticks` in stands; leaving runs it backwards at twice the speed. */
    public static float enteringTime(FormPhase phase, float ticks) {
        return switch (phase) {
            case ENTERING -> Math.min(ticks, ENTER_TICKS);
            case EXITING -> Math.max(ENTER_TICKS - ticks * 2.0F, 0.0F);
            default -> ENTER_TICKS;
        };
    }

    /** Whether a spell may be cast in the form: Iron's spells, not the Modified abilities (Gaunt Vessel itself excepted). */
    public static boolean castable(String spellId) {
        return !spellId.startsWith("mdvlcraft:") || spellId.equals(SPELL);
    }

    public static void enter(ServerPlayer player) {
        if (SERVER.containsKey(player.getUUID())) {
            return;
        }
        ServerPlayerPatch patch = patch(player);
        State state = new State(patch.isEpicFightMode());
        SERVER.put(player.getUUID(), state);
        patch.toVanillaMode(true);
        player.stopUsingItem();
        modifier(player, Attributes.MOVEMENT_SPEED, FROZEN, "Gaunt Vessel (changing)", -1.0, Operation.MULTIPLY_TOTAL);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
        sync(player, state, false);
    }

    /** Starts leaving the form (the entering played backwards); the form ends when it has played. */
    public static void leave(ServerPlayer player) {
        State state = SERVER.get(player.getUUID());
        if (state == null || state.phase == FormPhase.EXITING) {
            return;
        }
        state.phase = FormPhase.EXITING;
        state.ticks = 0;
        active(player, false);
        modifier(player, Attributes.MOVEMENT_SPEED, FROZEN, "Gaunt Vessel (changing)", -1.0, Operation.MULTIPLY_TOTAL);
        sync(player, state, false);
    }

    /** Ends the form at once: the player's own body, movement and Epic Fight mode come back. */
    private static void end(ServerPlayer player) {
        State state = SERVER.remove(player.getUUID());
        if (state == null) {
            return;
        }
        active(player, false);
        removeModifier(player, Attributes.MOVEMENT_SPEED, FROZEN);
        if (state.epicFightMode) {
            patch(player).toEpicFightMode(true);
        }
        BinderNetwork.sendToTrackingAndSelf(player, new GauntFormPacket(player.getId(), FormPhase.NONE, 0, false));
    }

    /** The form's own boons, on or off. */
    private static void active(ServerPlayer player, boolean on) {
        Attribute spirit = ForgeRegistries.ATTRIBUTES.getValue(SPIRIT_POWER);
        if (spirit == null) {
            throw new IllegalStateException("Attribute " + SPIRIT_POWER + " is missing from the pack");
        }
        if (on) {
            modifier(player, Attributes.MOVEMENT_SPEED, SPEED, "Gaunt Vessel", 0.25, Operation.MULTIPLY_BASE);
            modifier(player, spirit, SPELL_POWER, "Gaunt Vessel", 0.2, Operation.MULTIPLY_BASE);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED);
            removeModifier(player, spirit, SPELL_POWER);
            MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
            if (sight != null && sight.isInfiniteDuration()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    private static void sync(ServerPlayer player, State state, boolean cast) {
        BinderNetwork.sendToTrackingAndSelf(player, new GauntFormPacket(player.getId(), state.phase, state.ticks, cast));
    }

    private static ServerPlayerPatch patch(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }
        return patch;
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount, Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    /** The tearing into the Haunted Lord, between two points of the entering sequence, either way round (the bone cracks
     * are played by each client, client/GauntClient). */
    private static void sounds(ServerPlayer player, float from, float to) {
        if (SNAP_TICKS > Math.min(from, to) && SNAP_TICKS <= Math.max(from, to)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.4F, 0.7F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 0.6F);
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
        float before = enteringTime(state.phase, state.ticks);
        state.ticks++;
        sounds(player, before, enteringTime(state.phase, state.ticks));
        if (state.phase == FormPhase.ENTERING && state.ticks >= ENTER_TICKS) {
            state.phase = FormPhase.ACTIVE;
            state.ticks = 0;
            removeModifier(player, Attributes.MOVEMENT_SPEED, FROZEN);
            active(player, true);
            sync(player, state, false);
        } else if (state.phase == FormPhase.EXITING && state.ticks >= EXIT_TICKS) {
            end(player);
        }
    }

    /** No jumping while changing (both sides: the client moves the player). */
    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && changing(player)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.min(motion.y, 0.0), motion.z);
        }
    }

    private static boolean changing(Player player) {
        if (player.level().isClientSide()) {
            Seen seen = CLIENT.get(player.getId());
            return seen != null && seen.phase != FormPhase.ACTIVE;
        }
        State state = SERVER.get(player.getUUID());
        return state != null && state.phase != FormPhase.ACTIVE;
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
                BinderNetwork.sendTo(viewer, new GauntFormPacket(target.getId(), state.phase, state.ticks, false));
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

    /** Spells only: Iron's spells, not the Modified abilities. */
    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (in(event.getEntity()) && !castable(event.getSpellId())) {
            event.setCanceled(true);
            refuse(event.getEntity());
        }
    }

    /** A spell begun in the form plays the cast animation. */
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
        player.displayClientMessage(Component.translatable("ability.mdvlcraft.gaunt.spells_only").withStyle(ChatFormatting.RED), true);
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
