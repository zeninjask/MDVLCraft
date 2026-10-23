package com.mdvlcraft.binder.mixin.xaero;

import com.mdvlcraft.binder.gate.GateDimensions;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;

/**
 * No Xaero waypoints inside a gate (client/GateMapBlock hides the minimap there): render is the one entry for the
 * in-world waypoint markers and their on-screen labels, deathpoints included.
 */
@Mixin(value = WaypointsIngameRenderer.class, remap = false)
public abstract class WaypointsIngameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noWaypointsInGate(XaeroMinimapSession session, float partialTicks, MinimapProcessor processor,
            Matrix4f view, Matrix4f projection, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && GateDimensions.isGate(minecraft.level.dimension())) {
            ci.cancel();
        }
    }
}
