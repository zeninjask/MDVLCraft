package com.mdvlcraft.binder.wukong;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Animations.ReusableSources;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The core of Epic Fight - Wukong Moveset (p1nero, LGPL; animations from Black Myth: Wukong), ported to Epic Fight 20.14:
 * the staff's idle, walk, run and dash, its five-hit combo, dash and jump attacks, the charged smash (WukongSmashSkill)
 * and the four-way Wukong dodge. Files under assets/mdvlcraft/animmodels/animations/biped/wukong/.
 */
public final class WukongAnimations {
    static final Collider STAFF = new MultiOBBCollider(4, 0.2, 0.3, 1.8, 0.0, 0.0, 0.0);
    private static final Collider JUMP = new MultiOBBCollider(4, 0.8, 0.8, 0.8, 0.0, 0.9, 0.0);
    private static final Collider SMASH_SHORT = new MultiOBBCollider(4, 0.3, 0.3, 1.8, 0.0, 0.0, -0.4);
    private static final Collider SMASH_MID = new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);
    private static final Collider SMASH_LONG = new MultiOBBCollider(4, 0.3, 0.3, 3.4, 0.0, 0.0, -1.2);
    private static final Collider SMASH_FULL = new MultiOBBCollider(4, 0.6, 0.6, 4.3, 0.0, 0.0, -1.6);

    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<MovementAnimation> WALK;
    public static AnimationAccessor<MovementAnimation> RUN;
    public static AnimationAccessor<MovementAnimation> DASH;
    public static AnimationAccessor<StaticAnimation> JUMP_MOTION;
    public static AnimationAccessor<StaticAnimation> FALL;
    public static AnimationAccessor<BasicAttackAnimation> AUTO1;
    public static AnimationAccessor<BasicAttackAnimation> AUTO2;
    public static AnimationAccessor<BasicAttackAnimation> AUTO3;
    public static AnimationAccessor<BasicAttackAnimation> AUTO4;
    public static AnimationAccessor<BasicAttackAnimation> AUTO5;
    public static AnimationAccessor<BasicAttackAnimation> DASH_ATTACK;
    public static AnimationAccessor<AirSlashAnimation> JUMP_ATTACK;
    public static AnimationAccessor<StaticAnimation> SMASH_CHARGING;
    /** The charged smashes, weakest first: the longer the charge, the later one plays. */
    public static final java.util.List<AnimationAccessor<AttackAnimation>> SMASHES = new java.util.ArrayList<>();
    public static AnimationAccessor<DodgeAnimation> DODGE_FORWARD;
    public static AnimationAccessor<DodgeAnimation> DODGE_BACKWARD;
    public static AnimationAccessor<DodgeAnimation> DODGE_LEFT;
    public static AnimationAccessor<DodgeAnimation> DODGE_RIGHT;

    private WukongAnimations() {
    }

    private static HumanoidArmature biped() {
        return (HumanoidArmature)Armatures.BIPED.get();
    }

    /** Called by integration/BinderAnimations, the binder's one Epic Fight animation builder. */
    public static void build(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("biped/wukong/idle", accessor -> new StaticAnimation(true, accessor, Armatures.BIPED));
        WALK = builder.nextAccessor("biped/wukong/walk", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED));
        RUN = builder.nextAccessor("biped/wukong/run", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED));
        DASH = builder.nextAccessor("biped/wukong/dash", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED));
        JUMP_MOTION = builder.nextAccessor("biped/wukong/jump", accessor -> new StaticAnimation(0.15F, false, accessor, Armatures.BIPED));
        FALL = builder.nextAccessor("biped/wukong/fall", accessor -> new StaticAnimation(0.15F, true, accessor, Armatures.BIPED));

        AUTO1 = builder.nextAccessor("biped/wukong/auto_1", accessor -> speed(
            new BasicAttackAnimation(0.15F, 0.2916F, 0.5F, 0.51F, null, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.9F)), 1.8F));
        AUTO2 = builder.nextAccessor("biped/wukong/auto_2", accessor -> speed(
            new BasicAttackAnimation(0.15F, 0.6667F, 0.875F, 0.875F, null, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.25F)), 1.8F));
        AUTO3 = builder.nextAccessor("biped/wukong/auto_3", accessor -> speed(new BasicAttackAnimation(0.15F, accessor, Armatures.BIPED,
            new Phase(0.0F, 0.25F, 0.4583F, 0.4583F, 0.4583F, biped().toolR, null)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(4.0F)),
            new Phase(0.4583F, 0.4583F, 0.7083F, 0.7083F, 3.3333F, biped().toolR, null)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(4.0F))), 1.2F));
        AUTO4 = builder.nextAccessor("biped/wukong/auto_4", accessor -> speed(new BasicAttackAnimation(0.15F, accessor, Armatures.BIPED,
            spin(0.0F, 0.2F), spin(0.2F, 0.4F), spin(0.4F, 0.6F), spin(0.6F, 0.8F),
            new Phase(0.8F, 1.0416F, 1.125F, 1.2583F, 2.5F, biped().toolR, null)
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.5F))), 1.2F));
        AUTO5 = builder.nextAccessor("biped/wukong/auto_5", accessor -> speed(
            new BasicAttackAnimation(0.01F, 0.9166F, 1.15F, 1.9833F, null, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get())
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.01F, 1.9833F)), 1.2F));
        DASH_ATTACK = builder.nextAccessor("biped/wukong/auto_1_dash", accessor -> speed(
            new BasicAttackAnimation(0.15F, 0.2916F, 0.5F, 0.51F, null, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.9F)), 1.8F));
        JUMP_ATTACK = builder.nextAccessor("biped/wukong/jump_attack/jump_light_pre", accessor -> speed(
            new AirSlashAnimation(0.1F, 0.13F, 0.4F, 0.5F, JUMP, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.45F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1.0F)), 1.8F));

        SMASH_CHARGING = builder.nextAccessor("biped/wukong/smash/smash_charging", accessor -> new StaticAnimation(0.15F, true, accessor, Armatures.BIPED)
            .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, ReusableSources.CONSTANT_ONE));
        SMASHES.clear();
        SMASHES.add(smash(builder, 0, 0.75F, 0.92F, 1.67F, SMASH_SHORT, 1.4F, 2.0F, StunType.LONG));
        SMASHES.add(smash(builder, 1, 0.75F, 0.92F, 1.67F, SMASH_SHORT, 1.8F, 2.5F, StunType.LONG));
        SMASHES.add(smash(builder, 2, 1.3F, 1.55F, 2.5F, SMASH_MID, 2.2F, 3.0F, StunType.LONG));
        SMASHES.add(smash(builder, 3, 1.792F, 1.958F, 2.667F, SMASH_LONG, 2.6F, 3.5F, StunType.NEUTRALIZE));
        SMASHES.add(smash(builder, 4, 2.63F, 2.8F, 3.3F, SMASH_FULL, 3.0F, 4.0F, StunType.NEUTRALIZE));

        DODGE_FORWARD = dodge(builder, "f1");
        DODGE_BACKWARD = dodge(builder, "b1");
        DODGE_LEFT = dodge(builder, "l1");
        DODGE_RIGHT = dodge(builder, "r1");
    }

    private static Phase spin(float start, float end) {
        return new Phase(start, start, end, end, end, biped().toolR, null)
            .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.5F))
            .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_ROD.get());
    }

    private static AnimationAccessor<AttackAnimation> smash(AnimationBuilder builder, int level, float antic, float contact, float recovery,
                                                           Collider collider, float damage, float impact, StunType stun) {
        return builder.nextAccessor("biped/wukong/smash/smash_heavy" + level, accessor ->
            new AttackAnimation(0.15F, antic, antic, contact, recovery, collider, biped().toolR, accessor, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(impact))
                .addProperty(AttackPhaseProperty.STUN_TYPE, stun)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get())
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
                .addProperty(ActionAnimationProperty.CANCELABLE_MOVE, false)
                .addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.01F, antic)));
    }

    private static AnimationAccessor<DodgeAnimation> dodge(AnimationBuilder builder, String direction) {
        return builder.nextAccessor("biped/wukong/dodge/dodge_" + direction, accessor -> new DodgeAnimation(0.1F, 0.4F, accessor, 0.6F, 0.8F, Armatures.BIPED));
    }

    private static <A extends StaticAnimation> A speed(A animation, float speed) {
        return animation.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, current, prev, elapsed) -> speed);
    }
}
