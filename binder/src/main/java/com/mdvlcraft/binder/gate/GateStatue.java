package com.mdvlcraft.binder.gate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A trial gate's statue (rule Statues, plan 9.3): a stone figure that hunts the players in its room but moves only while
 * nobody is looking at it. While any player within SIGHT_RANGE has it in view (inside a cone around their look direction,
 * with a clear line of sight) its AI doesn't run at all: it stands still mid-stride and can't strike. It can't be hurt
 * (only /kill and creative players can remove it), only targets players inside its room (its restriction: the room's
 * centre and half-size, set by RuleGates when it places it) and walks back there when they leave. It removes itself
 * once its gate instance is gone. It is not a gate mob (no GateSpawner tag), so it doesn't count for drops or leashes.
 */
public class GateStatue extends Monster {
    /** Persistent NBT: the id of the gate instance this statue belongs to. */
    static final String INSTANCE = "mdvlcraft_gate_statue";
    private static final double SIGHT_RANGE = 64.0;
    /** cos of the half-angle of a player's view cone that counts as looking (about 63 degrees off centre). */
    private static final double SIGHT_COS = 0.45;
    private boolean frozen;

    public GateStatue(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.38)
            .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
            target -> this.isWithinRestriction(target.blockPosition())));
    }

    /** Frozen while watched: no goals, no steering, no momentum (isImmobile skips the AI step for the tick). */
    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            if (this.getTarget() != null && !this.isWithinRestriction(this.getTarget().blockPosition())) {
                this.setTarget(null);   // left the room: let them go
            }
            this.frozen = this.watched();
            if (this.frozen) {
                this.getNavigation().stop();
                this.setJumping(false);
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
            }
        }
        super.aiStep();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.frozen;
    }

    /** True if any player near enough has this statue in view with nothing in between. */
    boolean watched() {
        Vec3 centre = this.getBoundingBox().getCenter();
        for (Player player : this.level().players()) {
            if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(this) > SIGHT_RANGE * SIGHT_RANGE) {
                continue;
            }
            Vec3 toStatue = centre.subtract(player.getEyePosition()).normalize();
            if (toStatue.dot(player.getViewVector(1.0F)) > SIGHT_COS && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount % 40 == 0 && this.level() instanceof ServerLevel level) {
            GateInstances.Instance instance = GateInstances.get(level.getServer()).byId(this.getPersistentData().getInt(INSTANCE));
            if (!this.getPersistentData().contains(INSTANCE) || instance == null || !instance.live()) {
                this.discard();
            }
        }
    }

    /** Stone: nothing but /kill (or a creative player) hurts it. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer() || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }
}
