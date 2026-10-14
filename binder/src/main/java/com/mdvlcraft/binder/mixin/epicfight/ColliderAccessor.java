package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.collider.Collider;

@Mixin(value = Collider.class, remap = false)
public interface ColliderAccessor {
    @Accessor("modelCenter")
    Vec3 mdvlcraft$modelCenter();
}
