package com.mdvlcraft.binder.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft's start-up "Recovering from unsupported resolution" dialog is logged instead: it stops the game loading until
 * someone clicks OK, though the game has already resized itself to the window. It comes up when the window's size
 * changes while the game starts (Forge's loading window handing over at a scaled display).
 */
@Mixin(Minecraft.class)
public abstract class ResolutionPopupMixin {
    @Redirect(method = "<init>", require = 1, at = @At(value = "INVOKE", remap = false,
        target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z"))
    private boolean mdvlcraft$logResolutionPopup(CharSequence title, CharSequence message, CharSequence dialog, CharSequence icon, boolean ok) {
        LogUtils.getLogger().warn("{}", message);
        return true;
    }
}
