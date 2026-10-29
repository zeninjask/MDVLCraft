package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.epicskill.WeaponArtSkill;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/** A weapon with a weapon art (epicskill/WeaponArts) has that art as its innate skill, whatever its weapon type gives. */
@Mixin(value = {CapabilityItem.class, WeaponCapability.class}, remap = false)
public abstract class WeaponArtInnateMixin {
    @Inject(method = "getInnateSkill", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$weaponArt(PlayerPatch<?> playerpatch, ItemStack itemstack, CallbackInfoReturnable<Skill> cir) {
        Skill art = WeaponArtSkill.of(itemstack);
        if (art != null) {
            cir.setReturnValue(art);
        }
    }
}
