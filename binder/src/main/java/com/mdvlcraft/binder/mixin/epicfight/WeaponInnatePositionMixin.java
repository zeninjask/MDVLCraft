package com.mdvlcraft.binder.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;

/**
 * The weapon skill charge icon (32x32) sits in the gap between the Ares HUD hotbar (bottom left) and the Ares HUD middle
 * part, level with the middle part's diamond, instead of Epic Fight's bottom right corner. The numbers follow the pack's
 * Ares HUD layout (ares_hud-client.toml, Layout_DEFAULT): its positions are fractions of the screen and its sizes are
 * GUI pixels times the element's scale.
 */
@Mixin(value = BattleModeGui.class, remap = false)
public abstract class WeaponInnatePositionMixin {
    private static final int ICON = 32;
    // hotbarDefaultProperties x, plus the 2x5 hotbar's 106 px width at scale 0.80
    private static final double HOTBAR_X = 0.0011709601;
    private static final double HOTBAR_WIDTH = 106 * 0.8003640175;
    // centerHudDefaultProperties x and y; the diamond's centre is 9 px below the top
    private static final double CENTER_X = 0.3390625119;
    private static final double CENTER_Y = 0.8862275481;
    private static final int DIAMOND = 9;

    @Redirect(method = "renderWeaponInnateSkill",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/config/ClientConfig;getWeaponInnatePosition(II)Lyesman/epicfight/api/utils/math/Vec2i;"),
        require = 1)
    private Vec2i mdvlcraft$betweenHotbarAndCenter(int width, int height) {
        double left = HOTBAR_X * width + HOTBAR_WIDTH;
        double right = CENTER_X * width;
        int x = (int)Math.max(left, (left + right - ICON) / 2);
        int y = (int)(CENTER_Y * height) + DIAMOND - ICON / 2;
        return new Vec2i(x, y);
    }
}
