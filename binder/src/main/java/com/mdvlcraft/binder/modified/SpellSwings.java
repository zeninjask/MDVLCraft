package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * Spells that hit with the caster's weapon or body play an Epic Fight attack instead of Iron's arm-swing cast animation:
 * the held weapon's own combo swing (its finisher for heavy blows, its dash attack for dashing cuts), or a fist combo
 * hit for palms, punches and kicks. The Modified ones are listed here (and show no Iron's animation, ModifiedSpell);
 * any other mod's spell whose cast animation is one of Iron's melee swings gets a weapon swing too (its Iron's swing is
 * dropped on the client, mixin/irons/ClientSpellCastHelperMixin). The swing is only the look: while it plays, its own
 * hits do no damage (the spell already dealt its own).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SpellSwings {
    public enum Kind { SWING, HEAVY, DASH, FIST }

    private static final Map<String, Kind> MODIFIED = new HashMap<>();
    /** Iron's own melee cast animations (player side). */
    public static final Set<ResourceLocation> IRONS_SWINGS = Stream.of(SpellAnimations.SLASH_ANIMATION, SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION,
            SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION, SpellAnimations.ONE_HANDED_VERTICAL_UPSWING_ANIMATION)
        .map(AnimationHolder::getForPlayer).flatMap(java.util.Optional::stream).collect(Collectors.toSet());
    /** Players whose spell swing is playing, until this game time: the swing's own hits are cancelled. */
    private static final Map<UUID, Long> SWINGING = new HashMap<>();
    private static final int SWING_TICKS = 24;

    static {
        put(Kind.SWING, "cedar_cleave", "crimson_toll", "ember_garrote", "frozen_fingers", "kesa_guard", "pressure_point", "shakedown",
            "shatterstab", "coup_de_grace", "flensing_strike", "headsmans_verdict", "hyoga_draw", "jade_rupture", "mind_splinter",
            "raiko_fracture", "shackle_strike", "shiro_fracture", "slag_hammer", "soulfire_kanabo", "symmetry_slam", "thieving_talon",
            "marrow_pilfer", "unhorse", "flourish", "harvest_swing", "soul_cleave", "riptide_slash", "soul_reaper", "tetsubo_sweep",
            "shovel_slam", "sweep_the_legs");
        put(Kind.HEAVY, "kanabo_crush", "hammer_strike", "pickaxe_strike", "shield_bash", "warrior_shield_bash");
        put(Kind.DASH, "lunge", "kirisute_dash", "jade_iai", "asp_lunge", "twin_fang", "soul_rend", "iaido", "phantom_draw", "skewer",
            "frost_charge", "thunder_charge", "tide_charge");
        put(Kind.FIST, "palm_blood", "palm_eldritch", "palm_holy", "palm_ice", "palm_lightning", "palm_nature", "palm_soul", "palm_spirit",
            "palm_water", "stone_fist", "arc_palm", "sleight_palm", "hundred_fists", "tsuppari_barrage", "shatter_kick", "rooftop_vault",
            "wheel_of_agni", "whirlpool_kick", "back_alley_brawl", "walk_the_plank");
    }

    private SpellSwings() {
    }

    private static void put(Kind kind, String... keys) {
        for (String key : keys) {
            MODIFIED.put(key, kind);
        }
    }

    /** Whether a Modified ability (by key) swings instead of casting. */
    static boolean swings(String key) {
        return MODIFIED.containsKey(key);
    }

    @Nullable
    private static Kind kind(AbstractSpell spell) {
        ResourceLocation id = spell.getSpellResource();
        if (id.getNamespace().equals("mdvlcraft")) {
            return MODIFIED.get(id.getPath());
        }
        boolean melee = Stream.of(spell.getCastStartAnimation(), spell.getCastFinishAnimation())
            .map(AnimationHolder::getForPlayer).flatMap(java.util.Optional::stream).anyMatch(IRONS_SWINGS::contains);
        return melee ? Kind.SWING : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        Kind kind = spell != null ? kind(spell) : null;
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (kind == null || patch == null) {
            return;
        }
        List<AnimationAccessor<? extends AttackAnimation>> combo = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getAutoAttackMotion(patch);
        // a weapon's list is its combo hits, then its dash attack, then its air attack
        int hits = combo == null ? 0 : combo.size() - 2;
        AnimationAccessor<? extends AttackAnimation> swing;
        if (kind == Kind.FIST || hits < 1) {
            // (Epic Fight fills these in when its animations register, so they are read here, not at class load)
            List<AnimationAccessor<? extends AttackAnimation>> fists = List.of(Animations.FIST_AUTO1, Animations.FIST_AUTO2, Animations.FIST_AUTO3);
            swing = fists.get(player.getRandom().nextInt(fists.size()));
        } else if (kind == Kind.HEAVY) {
            swing = combo.get(hits - 1);
        } else if (kind == Kind.DASH) {
            swing = combo.get(hits);
        } else {
            swing = combo.get(player.getRandom().nextInt(hits));
        }
        SWINGING.put(player.getUUID(), player.level().getGameTime() + SWING_TICKS);
        patch.playAnimationSynchronized(swing, 0.0F);
    }

    /** The swing is only for show: its own hits land nothing. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource() instanceof EpicFightDamageSource && event.getSource().getEntity() instanceof ServerPlayer player) {
            Long until = SWINGING.get(player.getUUID());
            if (until != null && player.level().getGameTime() <= until) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SWINGING.remove(event.getEntity().getUUID());
    }
}
