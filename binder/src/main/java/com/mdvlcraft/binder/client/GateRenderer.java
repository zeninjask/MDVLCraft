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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The gate (gate/GateEntity): an upright oval of the animated nether portal texture, tinted blue (red once the gate turns
 * red), spinning, with a bright rim. While it stabilises it is small and flickers. Its label shows only when looked at.
 */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath("minecraft", "block/nether_portal");
    private static final int SEGMENTS = 32;

    public GateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderers event) -> event.registerEntityRenderer(GateItems.GATE.get(), GateRenderer::new));
    }

    @Override
    public void render(GateEntity gate, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(PORTAL);
        float time = gate.tickCount + partialTick;
        float halfWidth = gate.getBbWidth() / 2.0F;
        float halfHeight = gate.getBbHeight() / 2.0F;
        float size = gate.stable() ? 1.0F : 0.35F + 0.1F * Mth.sin(time * 0.4F);
        int r = gate.red() ? 255 : 80;
        int g = gate.red() ? 60 : 140;
        int b = gate.red() ? 60 : 255;
        pose.pushPose();
        pose.translate(0.0F, halfHeight, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-gate.getYRot()));
        pose.scale(halfWidth * size, halfHeight * size, 1.0F);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float spin = time * 0.05F;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            // the disc: a fan of degenerate quads from the centre, its texture turning
            vertex(buffer, matrix, normal, sprite, 0.0F, 0.0F, a0, 0.0F, spin, r, g, b, 200);
            vertex(buffer, matrix, normal, sprite, 0.0F, 0.0F, a0, 0.0F, spin, r, g, b, 200);
            vertex(buffer, matrix, normal, sprite, Mth.cos(a1), Mth.sin(a1), a1, 1.0F, spin, r, g, b, 200);
            vertex(buffer, matrix, normal, sprite, Mth.cos(a0), Mth.sin(a0), a0, 1.0F, spin, r, g, b, 200);
            // the rim
            float in = 0.94F;
            float out = 1.06F;
            int rr = Math.min(255, r + 120);
            int rg = Math.min(255, g + 100);
            int rb = Math.min(255, b + 60);
            vertex(buffer, matrix, normal, sprite, in * Mth.cos(a0), in * Mth.sin(a0), a0, 1.0F, -spin, rr, rg, rb, 255);
            vertex(buffer, matrix, normal, sprite, in * Mth.cos(a1), in * Mth.sin(a1), a1, 1.0F, -spin, rr, rg, rb, 255);
            vertex(buffer, matrix, normal, sprite, out * Mth.cos(a1), out * Mth.sin(a1), a1, 1.0F, -spin, rr, rg, rb, 255);
            vertex(buffer, matrix, normal, sprite, out * Mth.cos(a0), out * Mth.sin(a0), a0, 1.0F, -spin, rr, rg, rb, 255);
        }
        pose.popPose();
        super.render(gate, yaw, partialTick, pose, buffers, light);
    }

    /** A point at (x, y) on the unit oval; its texture coordinate is the polar point (angle + spin, radius) on the sprite. */
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, TextureAtlasSprite sprite, float x, float y,
                               float angle, float radius, float spin, int r, int g, int b, int a) {
        float u = 8.0F + 7.5F * radius * Mth.cos(angle + spin);
        float v = 8.0F + 7.5F * radius * Mth.sin(angle + spin);
        buffer.vertex(matrix, x, y, 0.0F).color(r, g, b, a).uv(sprite.getU(u), sprite.getV(v)).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    protected boolean shouldShowName(GateEntity gate) {
        return gate.hasCustomName() && gate == Minecraft.getInstance().crosshairPickEntity;
    }

    @Override
    public ResourceLocation getTextureLocation(GateEntity gate) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
