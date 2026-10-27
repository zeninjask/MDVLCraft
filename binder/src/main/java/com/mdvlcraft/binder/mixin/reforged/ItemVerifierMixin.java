package com.mdvlcraft.binder.mixin.reforged;

import com.mdvlcraft.binder.content.GearTiers;
import com.stereowalker.tiered.api.ItemVerifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The pack's Reforged pools name the tags mdvlcraft:tiered/<kind>; whether an item belongs is decided by GearTiers, not a tag file. */
@Mixin(value = ItemVerifier.class, remap = false)
public abstract class ItemVerifierMixin {
    @Shadow
    @Final
    private String tag;

    @Inject(method = "isValid(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$packGear(String itemId, CallbackInfoReturnable<Boolean> cir) {
        if (tag != null && tag.startsWith(GearTiers.TAG_PREFIX)) {
            cir.setReturnValue(GearTiers.matches(tag.substring(GearTiers.TAG_PREFIX.length()), ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId))));
        }
    }
}
