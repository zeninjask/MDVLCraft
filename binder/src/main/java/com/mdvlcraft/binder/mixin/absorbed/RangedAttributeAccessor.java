package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** absorbed/AttributeFix sets attributes' limits. */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
    @Accessor("minValue")
    @Mutable
    void mdvlcraft$setMinValue(double value);

    @Accessor("maxValue")
    @Mutable
    void mdvlcraft$setMaxValue(double value);
}
