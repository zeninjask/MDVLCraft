package com.mdvlcraft.binder.mixin.puffish;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.GuiGraphics;
import net.puffish.skillsmod.client.rendering.ConnectionBatchedRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skill tree connections drawn as gentle curves (Path of Exile style) instead of straight lines: a quadratic curve
 * bowed to one side by 10-20% of its length, drawn as short straight pieces with Pufferfish's own line strips. The bend
 * comes from the end points, which are the skills' tree coordinates, so it never changes while the view moves.
 * One-way connections keep Pufferfish's straight line and arrow (the binder's trees use only two-way ones).
 */
@Mixin(value = ConnectionBatchedRenderer.class, remap = false)
public abstract class ConnectionBatchedRendererMixin {
    private static final float STROKE = 3.0F;
    private static final float FILL = 1.0F;
    private static final float SEGMENT_LENGTH = 6.0F;

    @Shadow
    @Final
    private Int2ObjectMap<?> strokeBatch;

    @Shadow
    @Final
    private Int2ObjectMap<?> fillBatch;

    @Shadow
    private void emitLine(Int2ObjectMap<?> batch, Matrix4f matrix, int color, float startX, float startY, float endX, float endY, float thickness) {
        throw new AssertionError();
    }

    @Inject(method = "emitConnection", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$curve(GuiGraphics context, float startX, float startY, float endX, float endY, boolean bidirectional,
                                 int fillColor, int strokeColor, CallbackInfo ci) {
        if (!bidirectional) {
            return;
        }
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        int hash = 31 * (31 * (31 * Float.floatToIntBits(startX) + Float.floatToIntBits(startY)) + Float.floatToIntBits(endX))
                + Float.floatToIntBits(endY);
        float bend = (0.10F + 0.10F * ((hash >>> 1) % 1000) / 1000F) * ((hash & 1) == 0 ? 1 : -1);
        // Control point: the midpoint pushed sideways (perpendicular to the line) by bend * length.
        float cx = (startX + endX) / 2 - dy * bend;
        float cy = (startY + endY) / 2 + dx * bend;
        int segments = Math.max(4, Math.round(length / SEGMENT_LENGTH));
        Matrix4f matrix = context.pose().last().pose();
        float px = startX;
        float py = startY;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            float u = 1 - t;
            float x = u * u * startX + 2 * u * t * cx + t * t * endX;
            float y = u * u * startY + 2 * u * t * cy + t * t * endY;
            emitLine(strokeBatch, matrix, strokeColor, px, py, x, y, STROKE);
            emitLine(fillBatch, matrix, fillColor, px, py, x, y, FILL);
            px = x;
            py = y;
        }
        ci.cancel();
    }
}
