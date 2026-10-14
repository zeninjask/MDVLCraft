package com.mdvlcraft.binder.mixin.antimagic;

import com.mdvlcraft.binder.mixin.epicfight.ColliderAccessor;
import com.mdvlcraft.binder.mixin.epicfight.MultiColliderAccessor;
import com.mdvlcraft.binder.mixin.epicfight.OBBColliderAccessor;
import com.tiago.antimagic.item.DemonSlayerSword;
import com.tiago.antimagic.setup.ModItems;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Under the Anti-Magic Shroud (Black Divider) the Demon Slayer Sword is drawn 2.5 times its size (Anti-Magic's
 * ItemInHandRendererMixin scales it about the hand), so every attack swung with it hits with its hitbox 2.5 times as
 * big about the hand too: the attack's own collider or, without one, the weapon's. Box colliders (and the multi-box
 * sweeps Epic Fight's greatsword presets use) are scaled, each kept once per original; the sword's attacks use no
 * other kind, and any other is left as it is.
 */
@Mixin(value = AttackAnimation.Phase.class, remap = false)
public abstract class BlackDividerReachMixin {
    private static final double BLACK_DIVIDER_SCALE = 2.5;
    private static final Map<Collider, Collider> SCALED = new ConcurrentHashMap<>();

    @Shadow
    @Final
    public InteractionHand hand;

    @Redirect(method = "getCollidingEntities", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/api/collider/Collider;updateAndSelectCollideEntity(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/animation/types/AttackAnimation;FFLyesman/epicfight/api/animation/Joint;F)Ljava/util/List;"),
        require = 1)
    private List<Entity> mdvlcraft$blackDividerReach(Collider collider, LivingEntityPatch<?> patch, AttackAnimation animation,
                                                     float prevElapsedTime, float elapsedTime, Joint joint, float attackSpeed) {
        LivingEntity entity = patch.getOriginal();
        if (entity.getItemInHand(this.hand).is(ModItems.DEMON_SLAYER_SWORD.get()) && DemonSlayerSword.isBlackDividerActive(entity)) {
            collider = SCALED.computeIfAbsent(collider, BlackDividerReachMixin::scaled);
        }

        return collider.updateAndSelectCollideEntity(patch, animation, prevElapsedTime, elapsedTime, joint, attackSpeed);
    }

    private static Collider scaled(Collider collider) {
        if (collider instanceof MultiOBBCollider multi) {
            OBBCollider box = (OBBCollider)((MultiColliderAccessor)multi).mdvlcraft$colliders().get(0);
            Vec3 half = ((OBBColliderAccessor)box).mdvlcraft$modelVertices()[1].scale(BLACK_DIVIDER_SCALE);
            Vec3 center = ((ColliderAccessor)box).mdvlcraft$modelCenter().scale(BLACK_DIVIDER_SCALE);
            return new MultiOBBCollider(((MultiColliderAccessor)multi).mdvlcraft$numberOfColliders(), half.x, half.y, half.z, center.x, center.y, center.z);
        }

        if (collider instanceof OBBCollider box) {
            Vec3 half = ((OBBColliderAccessor)box).mdvlcraft$modelVertices()[1].scale(BLACK_DIVIDER_SCALE);
            Vec3 center = ((ColliderAccessor)box).mdvlcraft$modelCenter().scale(BLACK_DIVIDER_SCALE);
            return new OBBCollider(half.x, half.y, half.z, center.x, center.y, center.z);
        }

        return collider;
    }
}
