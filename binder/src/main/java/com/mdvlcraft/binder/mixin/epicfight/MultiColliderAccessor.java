package com.mdvlcraft.binder.mixin.epicfight;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiCollider;

@Mixin(value = MultiCollider.class, remap = false)
public interface MultiColliderAccessor {
    @Accessor("colliders")
    List<Collider> mdvlcraft$colliders();

    @Accessor("numberOfColliders")
    int mdvlcraft$numberOfColliders();
}
