package com.mdvlcraft.binder.superchef;

import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** A walk/run loop that plays faster the faster its owner moves (Super Chef's TDEMovementAnimation). */
public class ChefMovementAnimation extends MovementAnimation {
    private final float speed;

    public ChefMovementAnimation(AnimationAccessor<? extends MovementAnimation> accessor, AssetAccessor<? extends Armature> armature, float speed) {
        super(true, accessor, armature);
        this.speed = speed;
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
        if (animation.isLinkAnimation()) {
            return 1.0F;
        }
        LivingEntity entity = entitypatch.getOriginal();
        float movementSpeed = 1.0F;
        if (Math.abs(entity.walkAnimation.speed() - entity.walkAnimation.speed(1.0F)) < 0.007F) {
            movementSpeed *= entity.walkAnimation.speed() * this.speed;
        }
        return movementSpeed;
    }
}
