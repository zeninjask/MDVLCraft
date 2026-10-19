package com.mdvlcraft.binder.doppelganger;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

public class DoppelgangerPatch extends HumanoidMobPatch<DoppelgangerEntity> {
    private static final int RECENT_HIT_TICKS = 100;
    static final float DAMAGE_SHARE = 0.5F;
    private static final ThreadLocal<Boolean> DOUBLE_HIT = ThreadLocal.withInitial(() -> Boolean.FALSE);


    static void onLivingHurt(LivingHurtEvent event) {
        if (DOUBLE_HIT.get()) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    public DoppelgangerPatch() {
        super(Factions.NEUTRAL);
    }

    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        this.commonAggresiveMobAnimatorInit(animator);
    }

    public void updateMotion(boolean considerInaction) {
        this.commonAggressiveRangedMobUpdateMotion(considerInaction);
    }

    public void tick(LivingTickEvent event) {
        super.tick(event);
        PlayerPatch<?> owner = ((DoppelgangerEntity)this.getOriginal()).ownerPatch();
        if (owner != null && !this.isLogicalClient()) {
            LivingEntity target = ((DoppelgangerEntity)this.getOriginal()).sentTarget();
            if (target == null) {
                target = owner.getTarget();
            }

            LivingEntity self = (LivingEntity)owner.getOriginal();
            if (target == null && self.tickCount - self.getLastHurtMobTimestamp() < 100) {
                target = self.getLastHurtMob();
            }

            LivingEntity current = this.getTarget();
            LivingEntity wanted = target != null && target.isAlive() && target != owner.getOriginal() ? target : null;
            if (current != wanted) {
                this.setAttakTargetSync(wanted);
            }
        }
    }

    public EpicFightDamageSource getDamageSource(AnimationAccessor<? extends StaticAnimation> animation, InteractionHand hand) {
        PlayerPatch<?> owner = ((DoppelgangerEntity)this.getOriginal()).ownerPatch();
        return owner != null ? owner.getDamageSource(animation, hand) : super.getDamageSource(animation, hand);
    }

    private boolean mayHit(Entity target, @Nullable PlayerPatch<?> owner) {
        if (target == this.getTarget()) {
            return true;
        } else {
            return target instanceof Enemy ? true : owner != null && target instanceof Mob mob && mob.getTarget() == owner.getOriginal();
        }
    }

    public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
        PlayerPatch<?> owner = ((DoppelgangerEntity)this.getOriginal()).ownerPatch();
        if (target == this.getOriginal()
            || owner != null && target == owner.getOriginal()
            || target instanceof DoppelgangerEntity
            || !this.mayHit(target, owner)) {
            return AttackResult.missed(0.0F);
        }

        if (owner == null) {
            return super.attack(damageSource, target, hand);
        }

        target.invulnerableTime = 0;
        DOUBLE_HIT.set(Boolean.TRUE);

        try {
            return owner.attack(damageSource, target, hand);
        } finally {
            DOUBLE_HIT.set(Boolean.FALSE);
        }
    }
}
