package com.mdvlcraft.binder.mixin.unionlib;

import com.stereowalker.unionlib.config.ConfigBuilder;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * UnionLib records each mod registering a config in a plain list, from Forge's parallel mod construction: two mods at once
 * (UnionLib itself, Reforged, ...) could corrupt it and fail start-up with "Index 1 out of bounds for length 0". The
 * check-and-add now runs under a lock.
 */
@Mixin(value = ConfigBuilder.class, remap = false)
public abstract class ConfigBuilderRaceMixin {
    @Shadow
    private static List<String> modidList;

    @Inject(method = "handleConfigEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mdvlcraft$lockedAdd(String modid, CallbackInfo ci) {
        synchronized (ConfigBuilderRaceMixin.class) {
            if (!modidList.contains(modid)) {
                modidList.add(modid);
            }
        }
        ci.cancel();
    }
}
