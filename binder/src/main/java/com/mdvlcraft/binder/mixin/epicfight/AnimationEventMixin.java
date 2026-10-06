package com.mdvlcraft.binder.mixin.epicfight;

import org.arc.epic_ponder.entity.DummyPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Epic Fight Ponder plays weapon combos on a dummy actor that is not a Player. Some animation events of other mods
 * cast the animated entity to Player (Weapons of Miracles' Antitheus combo does, crashing the game when it is
 * pondered). For the Ponder actor only, an event that fails that cast is skipped and the scene plays on without it.
 */
@Mixin(value = AnimationEvent.class, remap = false)
public abstract class AnimationEventMixin {
    @Redirect(method = {"execute", "executeWithNewParams"}, at = @At(value = "INVOKE",
            target = "Lyesman/epicfight/api/animation/property/AnimationEvent$Event;fire(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/asset/AssetAccessor;Lyesman/epicfight/api/animation/property/AnimationParameters;)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mdvlcraft$skipPlayerOnlyEventsInPonder(AnimationEvent.Event event, LivingEntityPatch<?> patch,
                                                         AssetAccessor<? extends StaticAnimation> animation, AnimationParameters params) {
        if (!(patch.getOriginal() instanceof DummyPlayerEntity)) {
            event.fire(patch, animation, params);
            return;
        }
        try {
            event.fire(patch, animation, params);
        } catch (ClassCastException playerOnly) {
            // The event needs a real player; the Ponder actor plays the animation without it.
        }
    }
}
