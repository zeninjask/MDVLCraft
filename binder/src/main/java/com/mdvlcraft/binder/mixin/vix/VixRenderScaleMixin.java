package com.mdvlcraft.binder.mixin.vix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VIX (the effects library of Epic Fight Nightfall: Kusabimaru, Murasama, Yamato and other weapons' VFX) draws its
 * effects into buffers sized like the main framebuffer while the world renders, then lays them over the frame in
 * RenderPost, after the world. Render Scale draws the world into a smaller framebuffer of its own and only swaps the
 * full-size one back once the world is done, so the effects were laid over the full-size frame at the small size and
 * the whole picture ended up shrunk into a corner, on every screen near the effect. RenderPost now runs inside Render
 * Scale's scaled pass (its small framebuffer in place), and leaving that pass copies the result up to full size again.
 * Both mods are found by name: without either, this does nothing.
 */
@Pseudo
@Mixin(targets = "com.guhao.vix.client.pipeline.PostEffectPipelines", remap = false)
public abstract class VixRenderScaleMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Object renderScale;
    private static Method setShouldScale;
    private static Field configScale;
    private static Method getConfig;
    private static boolean looked;
    private static boolean scaling;

    @Inject(method = "RenderPost", at = @At("HEAD"), require = 0)
    private static void mdvlcraft$enterScaledPass(CallbackInfo ci) {
        if (scaled()) {
            scaling = set(true);
        }
    }

    @Inject(method = "RenderPost", at = @At("RETURN"), require = 0)
    private static void mdvlcraft$leaveScaledPass(CallbackInfo ci) {
        if (scaling) {
            scaling = false;
            set(false);
        }
    }

    /** Whether Render Scale is installed and set below full size. */
    private static boolean scaled() {
        if (!looked) {
            looked = true;
            try {
                Class<?> common = Class.forName("dev.zelo.renderscale.CommonClass");
                renderScale = common.getMethod("getInstance").invoke(null);
                setShouldScale = common.getMethod("setShouldScale", boolean.class);
                getConfig = common.getMethod("getConfig");
                configScale = Class.forName("dev.zelo.renderscale.config.RenderScaleConfig").getField("scale");
            } catch (ReflectiveOperationException | LinkageError e) {
                renderScale = null;
            }
        }
        if (renderScale == null) {
            return false;
        }
        try {
            return configScale.getFloat(getConfig.invoke(null)) < 0.999F;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean set(boolean on) {
        try {
            setShouldScale.invoke(renderScale, on);
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Render Scale pass for VIX effects failed", e);
            renderScale = null;
            return false;
        }
    }
}
