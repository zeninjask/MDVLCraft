package com.mdvlcraft.binder.mixin.majrusz;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Majrusz's "human" check (Misanthropy's targets) also covers Villager Recruits' and Village Recruits' people and the
 * Cursed Fate characters: recruits, Gojo, Sukuna, Naoya and Naobito.
 */
@Pseudo
@Mixin(targets = "com.majruszlibrary.entity.EntityHelper", remap = false)
public abstract class EntityHelperMixin {
    @Inject(method = "isHuman", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$moreHumans(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && entity != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (id != null && (id.getNamespace().equals("recruits") || id.getNamespace().equals("village_recruits")
                || id.getNamespace().equals("naoyaaddon") && id.getPath().endsWith("_npc")
                || id.getNamespace().equals("cursedfate") && id.getPath().startsWith("shinjuku_showdown_"))) {
                cir.setReturnValue(true);
            }
        }
    }
}
