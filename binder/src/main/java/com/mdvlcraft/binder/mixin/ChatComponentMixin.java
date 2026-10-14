package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.ChatPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The chat is drawn higher (client/ChatPosition), so the mouse height it reads clicks and hovers from moves with it. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(method = "screenToChatY", at = @At("HEAD"), argsOnly = true)
    private double mdvlcraft$raised(double y) {
        return y + ChatPosition.raise(Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }
}
