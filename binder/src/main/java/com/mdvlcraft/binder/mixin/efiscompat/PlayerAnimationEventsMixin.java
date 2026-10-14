package com.mdvlcraft.binder.mixin.efiscompat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Epic Fight x Iron's Spells cancels a cast unless ticksSinceLastAction > attack delay * castingDelay. The pack's
 * castingDelay is 0 (no delay), but that still cancels every cast on a tick the action timer was reset, and Stamina
 * Interactions Reworked resets it on every tick its stamina refills: spells (Demonic Assimilation and the rest) only
 * went off at full stamina, never just after sprinting. Counting one tick more makes the test ticks >= delay, so a
 * delay of 0 never blocks and a real delay works as before.
 */
@Pseudo
@Mixin(targets = "com.yukami.efiscompat.player.PlayerAnimationEvents", remap = false)
public abstract class PlayerAnimationEventsMixin {
    @Redirect(method = "canCastSpell", at = @At(value = "INVOKE",
        target = "Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;getTickSinceLastAction()I"), require = 1)
    private static int mdvlcraft$inclusiveDelay(ServerPlayerPatch patch) {
        return patch.getTickSinceLastAction() + 1;
    }
}
