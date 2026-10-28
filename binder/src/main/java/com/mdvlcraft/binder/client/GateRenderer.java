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
 * The gate (gate/GateEntity), pixel art like the gates of the Solo Leveling mods: a tall rounded rift of flat blue shades
 * (red once the gate turns red) whose spiral arms turn, inside a ragged dark outline. The frames are drawn by
 * tools/gates/gen_gate_portal.py, FRAMES of them stacked in one texture, and played one every TICKS_PER_FRAME ticks; the
 * art is drawn WIDTH times as wide as the gate is tall (the gate's own box is narrower). While it stabilises it is small
 * and pulses. Its label shows only when looked at.
 */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation BLUE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/gate_portal.png");
    private static final ResourceLocation RED = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/gate_portal_red.png");
    private static final int FRAMES = 16;
    private static final int TICKS_PER_FRAME = 3;
    private static final float WIDTH = 0.85F;

    public GateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderers event) -> event.registerEntityRenderer(GateItems.GATE.get(), GateRenderer::new));
    }

    @Override
    public void render(GateEntity gate, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = gate.tickCount + partialTick;
        float halfHeight = gate.getBbHeight() / 2.0F;
        float size = gate.stable() ? 1.0F : 0.35F + 0.1F * Mth.sin(time * 0.4F);
        int frame = (gate.tickCount / TICKS_PER_FRAME) % FRAMES;
        float v0 = frame / (float)FRAMES;
        float v1 = (frame + 1) / (float)FRAMES;
        pose.pushPose();
        pose.translate(0.0F, halfHeight, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-gate.getYRot()));
        pose.scale(halfHeight * WIDTH * size, halfHeight * size, 1.0F);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(gate.red() ? RED : BLUE));
        vertex(buffer, matrix, normal, -1.0F, -1.0F, 0.0F, v1);
        vertex(buffer, matrix, normal, 1.0F, -1.0F, 1.0F, v1);
        vertex(buffer, matrix, normal, 1.0F, 1.0F, 1.0F, v0);
        vertex(buffer, matrix, normal, -1.0F, 1.0F, 0.0F, v0);
        pose.popPose();
        super.render(gate, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v) {
        buffer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    protected boolean shouldShowName(GateEntity gate) {
        return gate.hasCustomName() && gate == Minecraft.getInstance().crosshairPickEntity;
    }

    @Override
    public ResourceLocation getTextureLocation(GateEntity gate) {
        return gate.red() ? RED : BLUE;
    }
}
