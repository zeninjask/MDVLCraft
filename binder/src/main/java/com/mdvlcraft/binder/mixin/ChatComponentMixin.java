package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.ChatPosition;
import com.mdvlcraft.binder.client.TypedChat;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The chat is drawn higher (client/ChatPosition), so the mouse height it reads clicks and hovers from moves with it. Chat lines
 * still being typed out draw only their letters so far (client/TypedChat).
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(method = "screenToChatY", at = @At("HEAD"), argsOnly = true)
    private double mdvlcraft$raised(double y) {
        return y + ChatPosition.raise(Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GuiMessage$Line;content()Lnet/minecraft/util/FormattedCharSequence;"))
    private FormattedCharSequence mdvlcraft$typedSoFar(GuiMessage.Line line) {
        return TypedChat.visible(line);
    }
}
