package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.epicskill.DemonDwellerSlashSkill;
import com.tiago.antimagic.item.DemonDwellerSword;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/** The Demon Dweller swings as a longsword but its innate skill is the Anti-Magic Slash (DemonDwellerSlashSkill). */
@Mixin(value = WeaponCapability.class, remap = false)
public abstract class DemonDwellerInnateMixin {
    @Inject(method = "getInnateSkill", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$antiMagicSlash(PlayerPatch<?> playerpatch, ItemStack itemstack, CallbackInfoReturnable<Skill> cir) {
        if (itemstack.getItem() instanceof DemonDwellerSword) {
            cir.setReturnValue(DemonDwellerSlashSkill.ANTI_MAGIC_SLASH);
        }
    }
}
