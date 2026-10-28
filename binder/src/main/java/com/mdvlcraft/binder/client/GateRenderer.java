package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gate.GateEntity;
import com.mdvlcraft.binder.gate.GateItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The gate (gate/GateEntity), drawn like the gates of Solo Leveling: an upright rift with a dark eye, its spiral arms
 * turning inward, a second lighter vortex turning the other way over it, a soft halo of light behind, and a ragged rim
 * that flickers. Blue, or red once the gate turns red. While it stabilises it is small and pulses. Textures from
 * tools/gates/gen_gate_portal.py. Its label shows only when looked at.
 */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation SWIRL = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/gate_swirl.png");
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/gate_halo.png");
    private static final int SEGMENTS = 48;

    public GateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderers event) -> event.registerEntityRenderer(GateItems.GATE.get(), GateRenderer::new));
    }

    @Override
    public void render(GateEntity gate, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = gate.tickCount + partialTick;
        float halfWidth = gate.getBbWidth() / 2.0F;
        float halfHeight = gate.getBbHeight() / 2.0F;
        float size = gate.stable() ? 1.0F + 0.015F * Mth.sin(time * 0.15F) : 0.35F + 0.1F * Mth.sin(time * 0.4F);
        boolean red = gate.red();
        pose.pushPose();
        pose.translate(0.0F, halfHeight, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-gate.getYRot()));
        pose.scale(halfWidth * size, halfHeight * size, 1.0F);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        // the halo: light spilling round the rift (additive), a little behind it
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(HALO));
        disc(glow, matrix, normal, 1.45F, -0.02F, 0.0F, red ? 255 : 70, red ? 50 : 150, red ? 40 : 255, 160, time, 0.0F);
        // the vortex, its arms turning inward, then a lighter one turning the other way over it
        VertexConsumer swirl = buffers.getBuffer(RenderType.entityTranslucentEmissive(SWIRL));
        disc(swirl, matrix, normal, 1.0F, 0.0F, time * 0.04F, red ? 230 : 60, red ? 40 : 110, red ? 40 : 255, 255, time, 0.025F);
        VertexConsumer over = buffers.getBuffer(RenderType.eyes(SWIRL));
        disc(over, matrix, normal, 0.92F, 0.01F, -time * 0.065F, red ? 255 : 140, red ? 120 : 210, red ? 110 : 255, 120, time, 0.0F);
        // the rim: a thin bright band whose edge wavers
        VertexConsumer rim = buffers.getBuffer(RenderType.eyes(HALO));
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float in = 0.9F;
            float out0 = 1.04F + edge(a0, time);
            float out1 = 1.04F + edge(a1, time);
            int r = red ? 255 : 170;
            int g = red ? 140 : 230;
            int b = red ? 120 : 255;
            vertex(rim, matrix, normal, in * Mth.cos(a0), in * Mth.sin(a0), 0.015F, 0.5F, 0.5F, r, g, b, 255);
            vertex(rim, matrix, normal, in * Mth.cos(a1), in * Mth.sin(a1), 0.015F, 0.5F, 0.5F, r, g, b, 255);
            vertex(rim, matrix, normal, out1 * Mth.cos(a1), out1 * Mth.sin(a1), 0.015F, 0.5F + 0.36F * Mth.cos(a1), 0.5F + 0.36F * Mth.sin(a1), r, g, b, 0);
            vertex(rim, matrix, normal, out0 * Mth.cos(a0), out0 * Mth.sin(a0), 0.015F, 0.5F + 0.36F * Mth.cos(a0), 0.5F + 0.36F * Mth.sin(a0), r, g, b, 0);
        }
        pose.popPose();
        super.render(gate, yaw, partialTick, pose, buffers, light);
    }

    /** How far the rim reaches past the disc at this angle: a slow ripple plus a fast flicker. */
    private static float edge(float angle, float time) {
        return 0.05F * Mth.sin(angle * 7.0F + time * 0.12F) + 0.03F * Mth.sin(angle * 13.0F - time * 0.31F);
    }

    /**
     * A fan of the texture over the unit oval at depth z, scaled by `scale`, its texture turned by `spin`. `wobble` bends
     * the outline with the rim's ripple.
     */
    private static void disc(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, float scale, float z, float spin,
                             int r, int g, int b, int a, float time, float wobble) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float s0 = scale * (1.0F + wobble * edge(a0, time) * 20.0F);
            float s1 = scale * (1.0F + wobble * edge(a1, time) * 20.0F);
            vertex(buffer, matrix, normal, 0.0F, 0.0F, z, 0.5F, 0.5F, r, g, b, a);
            vertex(buffer, matrix, normal, 0.0F, 0.0F, z, 0.5F, 0.5F, r, g, b, a);
            vertex(buffer, matrix, normal, s1 * Mth.cos(a1), s1 * Mth.sin(a1), z,
                0.5F + 0.5F * Mth.cos(a1 + spin), 0.5F + 0.5F * Mth.sin(a1 + spin), r, g, b, a);
            vertex(buffer, matrix, normal, s0 * Mth.cos(a0), s0 * Mth.sin(a0), z,
                0.5F + 0.5F * Mth.cos(a0 + spin), 0.5F + 0.5F * Mth.sin(a0 + spin), r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    protected boolean shouldShowName(GateEntity gate) {
        return gate.hasCustomName() && gate == Minecraft.getInstance().crosshairPickEntity;
    }

    @Override
    public ResourceLocation getTextureLocation(GateEntity gate) {
        return SWIRL;
    }
}
