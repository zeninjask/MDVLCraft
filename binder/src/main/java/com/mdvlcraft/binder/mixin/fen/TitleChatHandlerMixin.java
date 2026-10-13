package com.mdvlcraft.binder.mixin.fen;

import com.fen.mmorpgtitles.TitleData;
import net.minecraftforge.event.ServerChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Players without a class title chat in the vanilla format instead of "Name - : message". */
@Pseudo
@Mixin(targets = "com.fen.mmorpgtitles.TitleChatHandler", remap = false)
public abstract class TitleChatHandlerMixin {
    @Inject(method = "onChat", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$vanillaChatWithoutTitle(ServerChatEvent event, CallbackInfo ci) {
        if (TitleData.getDisplayed(event.getPlayer()).isBlank()) {
            ci.cancel();
        }
    }
}
