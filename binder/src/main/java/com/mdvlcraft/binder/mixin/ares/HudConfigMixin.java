package com.mdvlcraft.binder.mixin.ares;

import com.super_awesome_baby.ares_hud.client.config.HudConfig;
import com.super_awesome_baby.ares_hud.client.config.HudProperties;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ares HUD scales its bars, level diamond and value strip by the window size (0.73 and 0.98 at GUI scale 4), so their
 * pixels and the DQ font's came out unevenly sized. This rounds those scales to whole screen pixels per texture pixel
 * (0.75 and 1.0 at GUI scale 4), like the rest of the DQ look. The hotbar keeps Ares's own scale.
 */
@Mixin(value = HudConfig.class, remap = false)
public abstract class HudConfigMixin {
    @Inject(
        method = {
            "getCenterHudProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;",
            "getCenterSlotProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;Lcom/super_awesome_baby/ares_hud/client/config/BarSlot;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;",
            "getDynamicBarProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;Lcom/super_awesome_baby/ares_hud/client/config/DynamicStatusBar;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;",
            "getAuxiliaryGroupProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;Lcom/super_awesome_baby/ares_hud/client/config/AuxiliaryHudGroup;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;",
            "getXpBarProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;",
            "getStatusValueBarProperties(IILcom/super_awesome_baby/ares_hud/client/config/HotbarType;)Lcom/super_awesome_baby/ares_hud/client/config/HudProperties;"
        },
        at = @At("RETURN"),
        require = 6
    )
    private static void mdvlcraft$wholePixels(CallbackInfoReturnable<HudProperties> cir) {
        HudProperties properties = cir.getReturnValue();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        properties.scale = (float)(Math.max(1L, Math.round(properties.scale * guiScale)) / guiScale);
    }
}
