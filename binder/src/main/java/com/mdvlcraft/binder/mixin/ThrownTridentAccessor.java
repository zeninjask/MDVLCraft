package com.mdvlcraft.binder.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ThrownTrident's loyalty data key (specs/MartialSpecs: Return to Hand). */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
    @Accessor("ID_LOYALTY")
    static EntityDataAccessor<Byte> mdvlcraft$loyalty() {
        throw new AssertionError();
    }
}
