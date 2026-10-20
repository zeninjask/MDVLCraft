package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.mixin.epicfight.CombatBehaviorsBuilderAccessor;
import com.mdvlcraft.binder.mixin.epicfight.ConditionalWeaponInnateSkillAccessor;
import com.mdvlcraft.binder.mixin.epicfight.SimpleWeaponInnateSkillAccessor;
import com.mdvlcraft.binder.mixin.epicfight.WeaponCapabilityAccessor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.weaponinnate.ConditionalWeaponInnateSkill;
import yesman.epicfight.skill.weaponinnate.SimpleWeaponInnateSkill;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * Armed mobs use their weapon's Epic Fight skill. A mob has no skill slots or weapon charge, so Epic Fight's mob attacks
 * (MobCombatBehaviors) are plain swings for most weapons: a zombie or skeleton with a sword, axe or tool never does
 * anything else. Each mob's attacks (mixin/epicfight/HumanoidMobPatchMixin) get one more series that plays the held
 * weapon's skill move: a simple skill's attack (Sweeping Edge, the Guillotine, Heartpiercer, most modded weapons), a
 * conditional skill's first (Battojutsu), or the attack of a skill listed in {@link #EXTRA_MOVES}.
 * Skills built on a stance (Liechtenauer) or a grab (Blade Rush, the scythe) add nothing.
 *
 * mixin/epicfight/BehaviorSeriesMixin scales every series with a cooldown, this one too: weight 40 becomes 100 (an even
 * pick against the plain swings) and the 360-tick cooldown becomes 180 (9 seconds).
 */
public final class MobWeaponSkills {
    private static final float WEIGHT = 40.0F;
    private static final int COOLDOWN = 360;
    private static final double REACH = 3.0;
    /** Skills that aren't simple or conditional ones (Epic Fight Extra's own, Epic Fight's Steel Whirlwind): their attacks, by skill. */
    private static final Map<ResourceLocation, List<String>> EXTRA_MOVES = Map.of(
        extra("grasping_spire"), List.of("epicfightx:biped/skill/grasping_spire_first", "epicfightx:biped/skill/grasping_spire_second"),
        extra("eviscerate"), List.of("epicfightx:biped/skill/eviscerate_first", "epicfightx:biped/skill/eviscerate_second"),
        extra("steel_whirlwind"), List.of("epicfightx:biped/skill/steel_whirlwind"),
        // Dual Greatswords gives every greatsword Epic Fight's own Steel Whirlwind
        ResourceLocation.fromNamespaceAndPath("epicfight", "steel_whirlwind"), List.of("epicfight:biped/skill/steel_whirlwind"),
        extra("rushing_tempo"), List.of("epicfightx:biped/skill/rushing_tempo1"),
        extra("otachi_skill"), List.of("epicfightx:biped/skill/otachi_skill_01"),
        extra("pickaxe_skill"), List.of("epicfightx:biped/skill/pickaxe_skill"),
        extra("katana_skill"), List.of("epicfightx:biped/skill/uchigatana_skill_01")
    );

    private MobWeaponSkills() {
    }

    /** The mob's attacks with its weapon's skill added, or the attacks as they are when the weapon has none to use. */
    public static CombatBehaviors.Builder<HumanoidMobPatch<?>> withSkill(HumanoidMobPatch<?> mobpatch, CombatBehaviors.Builder<HumanoidMobPatch<?>> attacks) {
        List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> moves = skillMoves(mobpatch);
        if (moves.isEmpty()) {
            return attacks;
        }

        CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> skill = CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder()
            .weight(WEIGHT)
            .cooldown(COOLDOWN)
            .canBeInterrupted(false)
            .looping(false);
        for (AnimationManager.AnimationAccessor<? extends AttackAnimation> move : moves) {
            skill.nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(move).withinEyeHeight().withinDistance(0.0, REACH));
        }

        CombatBehaviors.Builder<HumanoidMobPatch<?>> withSkill = CombatBehaviors.builder();
        ((CombatBehaviorsBuilderAccessor)attacks).mdvlcraft$series().forEach(series -> ((CombatBehaviorsBuilderAccessor)withSkill).mdvlcraft$series().add(series));
        return withSkill.newBehaviorSeries(skill);
    }

    private static List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> skillMoves(HumanoidMobPatch<?> mobpatch) {
        CapabilityItem capability = mobpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        if (!(capability instanceof WeaponCapability weapon)) {
            return List.of();
        }

        var providers = ((WeaponCapabilityAccessor)weapon).mdvlcraft$innateSkill();
        Function<ItemStack, Skill> provider = providers.getOrDefault(weapon.getStyle(mobpatch), providers.get(CapabilityItem.Styles.COMMON));
        Skill skill = provider == null ? null : provider.apply(mobpatch.getOriginal().getMainHandItem());
        if (skill == null) {
            return List.of();
        } else if (EXTRA_MOVES.containsKey(skill.getRegistryName())) {
            return EXTRA_MOVES.get(skill.getRegistryName()).stream().map(MobWeaponSkills::extraAnimation).toList();
        } else if (skill instanceof SimpleWeaponInnateSkill simple) {
            return List.of(((SimpleWeaponInnateSkillAccessor)simple).mdvlcraft$animation());
        } else if (skill instanceof ConditionalWeaponInnateSkill conditional) {
            return List.of(((ConditionalWeaponInnateSkillAccessor)conditional).mdvlcraft$animations()[0]);
        }
        return List.of();
    }

    private static ResourceLocation extra(String path) {
        return ResourceLocation.fromNamespaceAndPath("epicfightx", path);
    }

    private static AnimationManager.AnimationAccessor<? extends AttackAnimation> extraAnimation(String id) {
        AnimationManager.AnimationAccessor<? extends AttackAnimation> animation = AnimationManager.byKey(ResourceLocation.parse(id));
        if (animation == null) {
            throw new IllegalStateException("No animation " + id);
        }
        return animation;
    }
}
