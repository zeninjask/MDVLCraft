package com.mdvlcraft.binder.mixin.epicfight;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.ScreenCalculations;

/**
 * The passive skill icons (Hyper Vitality's cooldown, Inner Growth, ...) stack upwards in a column just above the Ares
 * HUD hotbar (bottom left) instead of a row at Epic Fight's bottom right, where they overlapped the ability bar
 * (client/AbilityHud). The hotbar's left edge and top are fractions of the screen (hotbarDefaultProperties in the pack's
 * ares_hud-client.toml, Layout_DEFAULT), the same numbers as client/ChatPosition.
 */
@Mixin(value = BattleModeGui.class, remap = false)
public abstract class PassiveIconsPositionMixin {
    private static final int ICON = 24;
    private static final double HOTBAR_X = 0.0011709601;
    private static final double HOTBAR_TOP = 0.8694779277;
    private static final int GAP = 3;

    @Redirect(method = "renderNormalSkills",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/client/gui/ScreenCalculations$StartCoordGetter;get(IIIIILyesman/epicfight/client/gui/ScreenCalculations$HorizontalBasis;Lyesman/epicfight/client/gui/ScreenCalculations$VerticalBasis;)Lyesman/epicfight/api/utils/math/Vec2i;"),
        require = 1)
    private Vec2i mdvlcraft$aboveHotbar(ScreenCalculations.StartCoordGetter getter, int x, int y, int width, int height, int icons,
                                        ScreenCalculations.HorizontalBasis horBasis, ScreenCalculations.VerticalBasis verBasis) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        return new Vec2i((int)(HOTBAR_X * screenWidth) + GAP, (int)(HOTBAR_TOP * screenHeight) - GAP - ICON);
    }

    @Redirect(method = "renderNormalSkills",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/client/gui/ScreenCalculations$NextCoordGetter;getNext(Lyesman/epicfight/client/gui/ScreenCalculations$HorizontalBasis;Lyesman/epicfight/client/gui/ScreenCalculations$VerticalBasis;Lyesman/epicfight/api/utils/math/Vec2i;II)Lyesman/epicfight/api/utils/math/Vec2i;"),
        require = 1)
    private Vec2i mdvlcraft$upwards(ScreenCalculations.NextCoordGetter getter, ScreenCalculations.HorizontalBasis horBasis,
                                    ScreenCalculations.VerticalBasis verBasis, Vec2i previous, int width, int height) {
        return new Vec2i(previous.x, previous.y - ICON);
    }
}
