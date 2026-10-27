package com.mdvlcraft.binder.superchef;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InTimeEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.Side;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.JointColliderPair;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

/**
 * Epic Fight - Super Chef (merlin204 and P1nero), ported to Epic Fight 20.14: the skillet-and-spatula idle, walk, run,
 * sneak and kneel, and every attack of its key combo (ChefSkills). The left hand swings the skillet, the right the spatula.
 * Files under assets/mdvlcraft/animmodels/animations/biped/super_chef/; frame numbers are at 60 per second.
 */
public final class ChefAnimations {
    private static final Collider HIT = new MultiOBBCollider(3, 0.5, 1.0, 1.5, 0.0, 0.0, -1.0);
    private static final Collider IAIDO = new MultiOBBCollider(5, 1.0, 2.0, 20.0, 0.0, 0.0, -17.0);
    static final Collider SKILLET = HIT;

    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<ChefMovementAnimation> WALK;
    public static AnimationAccessor<ChefMovementAnimation> RUN;
    public static AnimationAccessor<ChefMovementAnimation> SNEAK;
    public static AnimationAccessor<ChefMovementAnimation> KNEE;
    public static AnimationAccessor<AttackAnimation> A, A_A, A_A_A, A_A_B, A_A_C, A_B, A_B_A, A_B_B, A_B_C;
    public static AnimationAccessor<AttackAnimation> B, B_A, B_A_A, B_A_B, B_A_C, B_B, B_B_A, B_B_B, B_B_C;
    public static AnimationAccessor<AttackAnimation> Y_A, Y_B, Y_C;

    private ChefAnimations() {
    }

    private static HumanoidArmature biped() {
        return (HumanoidArmature)Armatures.BIPED.get();
    }

    /** Called by integration/BinderAnimations, the binder's one Epic Fight animation builder. */
    public static void build(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("biped/super_chef/idle", accessor -> new StaticAnimation(true, accessor, Armatures.BIPED));
        WALK = builder.nextAccessor("biped/super_chef/walk", accessor -> new ChefMovementAnimation(accessor, Armatures.BIPED, 1.2F));
        RUN = builder.nextAccessor("biped/super_chef/run", accessor -> new ChefMovementAnimation(accessor, Armatures.BIPED, 1.6F));
        SNEAK = builder.nextAccessor("biped/super_chef/sneak", accessor -> new ChefMovementAnimation(accessor, Armatures.BIPED, 1.2F));
        KNEE = builder.nextAccessor("biped/super_chef/knee", accessor -> new ChefMovementAnimation(accessor, Armatures.BIPED, 1.2F));

        A = attack(builder, "a", 0.05F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(26, 36))), spatula(27, 35));
        A_A = attack(builder, "a_a", 0.15F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(21, 32))), spatula(22, 30));
        A_A_A = attack(builder, "a_a_a", 0.15F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(26, 34))), spatulaEnd(25, 33));
        A_A_B = attack(builder, "a_a_b", 0.15F, 1.0F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 42))), skillet(32, 40));
        A_A_C = attack(builder, "a_a_c", 0.05F, 1.1F, a -> {
            a.addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true);
            a.addEvents(InTimeEvent.create(0.75F, (AnimationEvent.E0)(patch, self, params) ->
                patch.getOriginal().playSound(EpicFightSounds.ENTITY_MOVE.get(), 1.0F, 1.0F), Side.SERVER));
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(26, 50).endPos(new Vec3(0.0, 0.1, -2.5)), ChefTrails.skillet(26, 50).endPos(new Vec3(0.0, 0.1, -2.5))));
        }, flurry(skillet(40, 48)), flurry(spatula(45, 50)));
        B = attack(builder, "b", 0.05F, 1.1F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 42))), skillet(30, 40));
        B_A = attack(builder, "b_a", 0.05F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(30, 42))), spatula(32, 40));
        B_A_A = attack(builder, "b_a_a", 0.05F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(26, 36))), spatula(28, 35));
        B_A_B = attack(builder, "b_a_b", 0.05F, 1.0F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 42))), skillet(32, 40));
        B_A_C = attack(builder, "b_a_c", 0.05F, 1.0F, a -> {
            a.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.5F));
            a.addProperty(ActionAnimationProperty.CANCELABLE_MOVE, false);
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(30, 50).startPos(new Vec3(0.0, 0.1, 10.0)).endPos(new Vec3(0.0, -0.1, -100.0))));
        }, iaido(40, 45));
        A_B = attack(builder, "a_b", 0.15F, 1.0F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 42))), skillet(32, 40));
        A_B_A = attack(builder, "a_b_a", 0.05F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(24, 36))), spatula(26, 35));
        A_B_B = attack(builder, "a_b_b", 0.05F, 1.0F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 42))), skillet(31, 40));
        A_B_C = attack(builder, "a_b_c", 0.05F, 1.0F, a -> {
            a.addProperty(AttackPhaseProperty.STUN_TYPE, StunType.FALL);
            a.addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(6.0F));
            a.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true);
            a.addEvents(groundSplit(58, 1.5, 0.0, 6.0F));
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(20, 60).endPos(new Vec3(0.0, 0.1, -2.5)), ChefTrails.skillet(20, 60).endPos(new Vec3(0.0, 0.1, -2.5))));
        }, skillet(28, 35), skilletEnd(54, 57));
        B_B = attack(builder, "b_b", 0.05F, 1.1F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 46))), skillet(35, 45));
        B_B_A = attack(builder, "b_b_a", 0.05F, 1.2F, a -> trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(45, 37))), spatula(26, 35));
        B_B_B = attack(builder, "b_b_b", 0.05F, 0.9F, a -> {
            a.addEvents(groundSplit(35, 1.2, 0.0, 3.0F));
            trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(30, 37)));
        }, skillet(31, 35));
        B_B_C = attack(builder, "b_b_c", 0.05F, 0.8F, a ->
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(25, 40).startPos(new Vec3(0.0, 0.1, 10.0)).endPos(new Vec3(0.0, 0.1, -100.0)))), iaido(25, 35));
        Y_A = attack(builder, "y_a", 0.15F, 0.8F, a -> {
            a.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true);
            a.addEvents(groundSplit(23, 0.0, 0.0, 5.0F), fire(0.05F), fire(0.1F), fire(0.15F));
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(3, 20).endPos(new Vec3(0.0, 0.1, -2.5)), ChefTrails.skillet(3, 20).endPos(new Vec3(0.0, 0.1, -2.5))));
        }, both(3, 15, 33));
        Y_B = attack(builder, "y_b", 0.15F, 0.8F, a -> {
            a.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true);
            a.addEvents(groundSplit(25, 0.0, 0.0, 5.0F), fire(0.05F), fire(0.1F), fire(0.15F));
            trails(a, () -> ChefTrails.add(a, ChefTrails.spatula(3, 20).endPos(new Vec3(0.0, 0.1, -2.5)), ChefTrails.skillet(3, 20).endPos(new Vec3(0.0, 0.1, -2.5))));
        }, both(3, 15, 33));
        // Wok of Passion: untouchable while it plays, and it spends the skill's stack
        Y_C = attack(builder, "y_c", 0.15F, 1.0F, a -> {
            a.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true);
            a.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(0.0F, Float.MAX_VALUE));
            a.newTimePair(0.0F, Float.MAX_VALUE);
            a.addStateRemoveOld(EntityState.ATTACK_RESULT, source -> AttackResult.ResultType.MISSED);
            a.addEvents(groundSplit(80, 15.0, -12.0, 15.0F), InTimeEvent.create(0.01F, (AnimationEvent.E0)(patch, self, params) -> {
                if (patch instanceof ServerPlayerPatch player) {
                    SkillContainer innate = player.getSkill(SkillSlots.WEAPON_INNATE);
                    innate.getSkill().setStackSynchronize(innate, 0);
                }
            }, Side.SERVER));
            trails(a, () -> ChefTrails.add(a, ChefTrails.skillet(3, 85).startPos(new Vec3(0.0, 0.1, 20.0)).endPos(new Vec3(0.0, 0.1, -70.0))));
        }, both(50, 85, 180));
    }

    @SafeVarargs
    private static AnimationAccessor<AttackAnimation> attack(AnimationBuilder builder, String name, float convert, float speed,
                                                            java.util.function.Consumer<AttackAnimation> extra, Supplier<Phase>... phases) {
        return builder.nextAccessor("biped/super_chef/" + name, accessor -> {
            AttackAnimation animation = new AttackAnimation(convert, accessor, Armatures.BIPED, Arrays.stream(phases).map(Supplier::get).toArray(Phase[]::new));
            animation.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, current, prev, elapsed) -> speed);
            extra.accept(animation);
            return animation;
        });
    }

    private static void trails(AttackAnimation animation, Runnable client) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            client.run();
        }
    }

    private static Phase phase(int startFrame, int endFrame, boolean holdsToEnd, InteractionHand hand, Joint joint, float damage) {
        float start = startFrame / 60.0F;
        float end = endFrame / 60.0F;
        return new Phase(0.0F, start, start, end, holdsToEnd ? Float.MAX_VALUE : end, Float.MAX_VALUE, false, hand, joint, HIT)
            .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
    }

    private static Supplier<Phase> spatula(int start, int end) {
        return () -> phase(start, end, false, InteractionHand.MAIN_HAND, biped().toolR, 0.7F);
    }

    private static Supplier<Phase> spatulaEnd(int start, int end) {
        return () -> phase(start, end, true, InteractionHand.MAIN_HAND, biped().toolR, 1.1F);
    }

    private static Supplier<Phase> skillet(int start, int end) {
        return () -> phase(start, end, false, InteractionHand.OFF_HAND, biped().toolL, 1.3F);
    }

    private static Supplier<Phase> skilletEnd(int start, int end) {
        return () -> phase(start, end, true, InteractionHand.OFF_HAND, biped().toolL, 1.3F);
    }

    private static Supplier<Phase> flurry(Supplier<Phase> phase) {
        return () -> phase.get().addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get())
            .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.multiplier(10.0F));
    }

    /** The long dash cut that strikes everything along a 20-block line ahead. */
    private static Supplier<Phase> iaido(int contactFrame, int recoveryFrame) {
        float contact = contactFrame / 60.0F;
        float recovery = recoveryFrame / 60.0F;
        return () -> new Phase(0.0F, contact, contact, recovery, recovery, Float.MAX_VALUE, false, InteractionHand.MAIN_HAND, biped().toolR, IAIDO)
            .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
            .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLADE_RUSH_FINISHER.get());
    }

    /** Both hands at once (the leaping slams). */
    private static Supplier<Phase> both(int startFrame, int contactFrame, int recoveryFrame) {
        float start = startFrame / 60.0F;
        return () -> new Phase(0.0F, start, start, contactFrame / 60.0F, recoveryFrame / 60.0F, Float.MAX_VALUE, false, InteractionHand.MAIN_HAND,
            JointColliderPair.of(biped().toolR, null), JointColliderPair.of(biped().toolL, null))
            .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.8F));
    }

    private static Vec3 jointWorldPos(LivingEntityPatch<?> patch, Joint joint) {
        Pose pose = patch.getAnimator().getPose(0.5F);
        Vec3 pos = patch.getOriginal().position();
        OpenMatrix4f model = OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z)
            .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS).mulBack(patch.getModelMatrix(1.0F)));
        OpenMatrix4f jointTransform = new OpenMatrix4f(patch.getArmature().getBoundTransformFor(pose, joint)).mulFront(model);
        return OpenMatrix4f.transform(jointTransform, Vec3.ZERO);
    }

    /** Flames burst from the skillet and spatula. */
    private static InTimeEvent<AnimationEvent.E0> fire(float time) {
        return InTimeEvent.create(time, (AnimationEvent.E0)(patch, self, params) -> {
            if (patch.getOriginal().level() instanceof ServerLevel level) {
                for (Joint joint : List.of(biped().toolR, biped().toolL)) {
                    Vec3 pos = jointWorldPos(patch, joint);
                    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.2, pos.z, 50, 0.1, 0.1, 0.1, 1.0);
                }
                level.playSound(null, patch.getOriginal().blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }, Side.SERVER);
    }

    /** A slam viewOffset blocks ahead: cracks the ground, burns and hurts everything within radius, flames on the client. */
    private static InTimeEvent<AnimationEvent.E0> groundSplit(int frame, double viewOffset, double yOffset, float radius) {
        return InTimeEvent.create(frame / 60.0F, (AnimationEvent.E0)(patch, self, params) -> groundSplit(patch, viewOffset, yOffset, radius), Side.BOTH);
    }

    private static void groundSplit(LivingEntityPatch<?> patch, double viewOffset, double yOffset, float radius) {
        LivingEntity entity = patch.getOriginal();
        Vec3 pos = entity.position();
        double yaw = Math.toRadians(patch.getYRot());
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).normalize().scale(viewOffset);
        Vec3 ground = pos.add(ahead.x, -1.0 + yOffset, ahead.z);
        Vec3 centre = pos.add(ahead.x, yOffset, ahead.z);
        if (entity.level() instanceof ServerLevel level) {
            for (int y = (int)(ground.y + radius); y > ground.y - radius; y--) {
                if (!level.getBlockState(BlockPos.containing(ground.x, y, ground.z)).isAir()) {
                    LevelUtil.circleSlamFracture(entity, level, ground, radius, false);
                    break;
                }
            }
            areaDamage(level, centre, entity, (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE), radius);
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F * radius, 1.0F);
            return;
        }
        RandomSource random = entity.getRandom();
        int count = (int)(80.0F * radius / 6.0F);
        OpenMatrix4f flat = new OpenMatrix4f().rotate((float)Math.toRadians(90.0), new Vec3f(1.0F, 0.0F, 0.0F));
        for (int i = 0; i < count * 3; i++) {
            double theta = Math.PI * 2 * random.nextDouble();
            double phi = (random.nextDouble() - 0.5) * Math.PI * 0.05 / 0.6;
            float spread = i < count * 2 ? 1.0F : (random.nextFloat() + 0.5F) * 0.8F;
            Vec3f direction = new Vec3f((float)(0.6 * Math.cos(phi) * Math.cos(theta)) * spread, (float)(0.6 * Math.cos(phi) * Math.sin(theta)) * spread, (float)(0.6 * Math.sin(phi)) * spread);
            OpenMatrix4f.transform3v(flat, direction, direction);
            entity.level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, direction.x, direction.y, direction.z);
        }
        for (int i = 0; i < 60.0F * radius / 6.0F; i++) {
            entity.level().addParticle(ParticleTypes.FLAME, pos.x + (random.nextFloat() - 0.5F) * 1.2F, pos.y + 0.2F, pos.z + (random.nextFloat() - 0.5F) * 1.2F,
                0.0, random.nextFloat() * 1.5F, 0.0);
        }
    }

    private static void areaDamage(ServerLevel level, Vec3 centre, LivingEntity source, float damage, float radius) {
        if (radius <= 0.0F) {
            return;
        }
        AABB area = new AABB(centre, centre).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e.isAlive() && e != source && !e.isAlliedTo(source) && e.distanceToSqr(centre) <= radius * radius)) {
            target.invulnerableTime = 0;
            target.hurt(EpicFightDamageSources.shockwave(source).setInitialPosition(centre).addRuntimeTag(EpicFightDamageTypeTags.FINISHER)
                .setStunType(StunType.LONG).setBaseImpact(damage / 5.0F), damage * 0.5F);
            target.invulnerableTime = 0;
            target.hurt(source.damageSources().mobAttack(source), damage);
            target.setSecondsOnFire(5);
        }
    }
}
