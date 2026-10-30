package com.mdvlcraft.binder.mixin.traits;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Entity's shared flags (on fire, glowing...), for a glow only one player sees (traits/TraitMore.glow). */
@Mixin(Entity.class)
public interface EntityFlagsAccessor {
    @Accessor("DATA_SHARED_FLAGS_ID")
    static EntityDataAccessor<Byte> mdvlcraft$flags() {
        throw new AssertionError();
    }
}
