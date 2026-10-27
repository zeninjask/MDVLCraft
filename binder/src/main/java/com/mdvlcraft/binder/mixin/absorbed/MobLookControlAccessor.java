package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** absorbed/FastLookControl swaps mobs' look controls. */
@Mixin(Mob.class)
public interface MobLookControlAccessor {
    @Accessor("lookControl")
    @Mutable
    void mdvlcraft$setLookControl(LookControl control);
}
