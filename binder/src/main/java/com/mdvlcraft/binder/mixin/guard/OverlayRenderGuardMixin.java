package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.mixin.PoseStackAccessor;
import com.mdvlcraft.binder.util.CrashGuard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** An error in a HUD overlay (any mod's) is logged instead of crashing the client; that overlay is skipped that frame. */
@Mixin(value = ForgeGui.class, remap = false)
public abstract class OverlayRenderGuardMixin {
    @Redirect(method = "lambda$render$0", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/client/gui/overlay/IGuiOverlay;render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V"), require = 0)
    private void mdvlcraft$guard(IGuiOverlay overlay, ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        int depth = ((PoseStackAccessor) graphics.pose()).mdvlcraft$poses().size();
        try {
            overlay.render(gui, graphics, partialTick, width, height);
        } catch (Throwable error) {
            if (!CrashGuard.catchable(error)) {
                throw error;
            }
            while (((PoseStackAccessor) graphics.pose()).mdvlcraft$poses().size() > depth) {
                graphics.pose().popPose();
            }
            CrashGuard.caught("HUD overlay", overlay.getClass().getName(), error);
        }
    }
}
