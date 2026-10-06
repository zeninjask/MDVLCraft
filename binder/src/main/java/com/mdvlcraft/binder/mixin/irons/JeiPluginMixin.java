package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.jei.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iron's JEI plugin only adds pages for its own (disabled) blocks and items: arcane anvil, scroll forge, alchemist
 * cauldron, item repairs and a page per spell scroll. Building them took 20+ seconds on join, long enough for slow
 * clients to time out, so none of it is registered.
 */
@Mixin(value = JeiPlugin.class, remap = false)
public abstract class JeiPluginMixin {
    @Inject(method = "registerItemSubtypes", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipSubtypes(ISubtypeRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerCategories", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipCategories(IRecipeCategoryRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerRecipes", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipRecipes(IRecipeRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerGuiHandlers", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipGuiHandlers(IGuiHandlerRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerRecipeTransferHandlers", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipTransferHandlers(IRecipeTransferRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerRecipeCatalysts", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipCatalysts(IRecipeCatalystRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "registerAdvanced", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipAdvanced(IAdvancedRegistration registration, CallbackInfo ci) {
        ci.cancel();
    }
}
