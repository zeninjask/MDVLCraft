package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.epicskill.ShunpoSetup;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Epic Fight leaves the guard key to a held shield (it skips the guard skill when either hand holds one). Shunpo's owner
 * cannot raise a shield (ShunpoSetup.NoShield), so with Shunpo equipped the guard key always goes to Shunpo.
 */
@Mixin(value = ControlEngine.class, remap = false)
public abstract class ControlEngineShunpoMixin {
    @Redirect(method = "maybeGuard", at = @At(value = "INVOKE", target = "Ljava/lang/Class;isAssignableFrom(Ljava/lang/Class;)Z"), require = 2)
    private boolean mdvlcraft$shunpoIgnoresShield(Class<?> shield, Class<?> held) {
        PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(Minecraft.getInstance().player, PlayerPatch.class);
        SkillContainer guard = patch == null ? null : patch.getSkill(SkillSlots.GUARD);
        return (guard == null || guard.getSkill() != ShunpoSetup.SHUNPO) && shield.isAssignableFrom(held);
    }
}
