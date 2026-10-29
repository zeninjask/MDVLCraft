package com.mdvlcraft.binder.mixin.wom;

import java.util.UUID;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

/**
 * Critical Knowledge (Weapons of Miracles) no longer rolls its own crits: it raises the chance and damage of the pack's
 * one roll instead (combat/Crits), so its crits are the same crits everything else sees.
 */
@Pseudo
@Mixin(targets = "reascer.wom.skill.passive.CriticalKnowledgeSkill", remap = false)
public abstract class CriticalKnowledgeMixin {
    @Redirect(method = "onInitiate", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/entity/eventlistener/PlayerEventListener;addEventListener(Lyesman/epicfight/world/entity/eventlistener/PlayerEventListener$EventType;Ljava/util/UUID;Ljava/util/function/Consumer;)V"),
        require = 1)
    private void mdvlcraft$noOwnRoll(PlayerEventListener listener, PlayerEventListener.EventType<?> type, UUID id, Consumer<?> action) {
    }
}
