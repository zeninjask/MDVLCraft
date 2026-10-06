package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Medieval skill screen: Pufferfish's Skills borrows the vanilla advancement window and tabs; point it at the binder's
 * dark oak, gold and parchment versions instead (same layout; drawn by tools/generate_textures.py). The vanilla
 * advancements screen keeps its own look. Node frames, lines and backgrounds are set in the skill tree data.
 */
@Mixin(value = SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {
    @Shadow
    @Final
    @Mutable
    private static ResourceLocation WINDOW_TEXTURE;

    @Shadow
    @Final
    @Mutable
    private static ResourceLocation TABS_TEXTURE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mdvlcraft$medievalTextures(CallbackInfo ci) {
        WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/skills/window.png");
        TABS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/skills/tabs.png");
    }
}
