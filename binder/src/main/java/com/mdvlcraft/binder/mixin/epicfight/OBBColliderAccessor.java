package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.collider.OBBCollider;

@Mixin(value = OBBCollider.class, remap = false)
public interface OBBColliderAccessor {
    /** Corner 1 is (half width, half height, half length). */
    @Accessor("modelVertices")
    Vec3[] mdvlcraft$modelVertices();
}
