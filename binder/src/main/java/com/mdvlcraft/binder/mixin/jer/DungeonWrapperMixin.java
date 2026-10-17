package com.mdvlcraft.binder.mixin.jer;

import com.mdvlcraft.binder.compat.jer.JerLoot;
import com.mojang.blaze3d.vertex.PoseStack;
import jeresources.jei.dungeon.DungeonWrapper;
import jeresources.util.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A chest page's title shrinks to fit beside the chest, and the mod it belongs to is named under the stack count. */
@Mixin(value = DungeonWrapper.class, remap = false)
public abstract class DungeonWrapperMixin {
    /** The page is 163 wide and the title starts at 60. */
    private static final float TITLE_ROOM = 101.0F;
    /** JER's small font is drawn at three quarters size. */
    private static final float SMALL_SCALE = 0.75F;

    @Redirect(
        method = "drawInfo",
        at = @At(value = "INVOKE", target = "Ljeresources/util/Font;print(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;II)V", ordinal = 0)
    )
    private void mdvlcraft$fitTitle(Font font, GuiGraphics graphics, String title, int x, int y) {
        fit(font, 1.0F, graphics, title, x, y);
    }

    /** The mod's name on its own small line under the stack count. */
    @Inject(method = "drawInfo", at = @At("TAIL"))
    private void mdvlcraft$source(int recipeWidth, int recipeHeight, GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo ci) {
        String source = JerLoot.chestSource(((DungeonEntryAccessor)((DungeonWrapper)(Object)this).chest).mdvlcraft$name());
        if (!source.isEmpty()) {
            fit(Font.small, SMALL_SCALE, graphics, source, 60, 29);
        }
    }

    private static void fit(Font font, float fontScale, GuiGraphics graphics, String text, int x, int y) {
        float width = Minecraft.getInstance().font.width(text) * fontScale;
        if (width <= TITLE_ROOM) {
            font.print(graphics, text, x, y);
            return;
        }

        float scale = TITLE_ROOM / width;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y + (1.0F - scale) * 4.0F * fontScale, 0.0F);
        pose.scale(scale, scale, 1.0F);
        font.print(graphics, text, 0, 0);
        pose.popPose();
    }
}
