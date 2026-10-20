package com.mdvlcraft.binder.mixin.epicfight;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

@Mixin(value = CombatBehaviors.Builder.class, remap = false)
public interface CombatBehaviorsBuilderAccessor {
    @Accessor("behaviorSeriesList")
    List<CombatBehaviors.BehaviorSeries.Builder<?>> mdvlcraft$series();
}
