package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gaunt.GauntLeap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import sonnenlichts.tje.client.config.TjeModConfig;
import sonnenlichts.tje.client.util.ModUtils;

/**
 * The path a charging Haunted Lord's leap will take (gaunt/GauntLeap), drawn the way Trajectory Estimation draws an arrow's
 * (its points and colours from its config, its red marker where the path meets a block). Only loaded when that mod is
 * installed (GauntClient). The path is the player's centre, one point per tick plus two between, under vanilla's air drag
 * and gravity, until it meets a block or 10 s pass.
 */
final class LeapTrajectory {
    private static final int TICKS = 200;
    private static final int BETWEEN = 3;

    private LeapTrajectory() {
    }

    static void render(RenderLevelStageEvent event, Player player, float charge) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack matrix = event.getPoseStack();
        BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer builder = buffers.getBuffer(ModUtils.BUFFS);
        Vec3 view = event.getCamera().getPosition();
        Vec3 position = player.getPosition(event.getPartialTick()).add(0.0, player.getBbHeight() / 2.0, 0.0);
        Vec3 velocity = player.getViewVector(event.getPartialTick()).scale(GauntLeap.speed(charge));
        float size = (float)TjeModConfig.cubeSize;
        for (int tick = 0; tick < TICKS; tick++) {
            Vec3 next = position.add(velocity);
            BlockHitResult hit = player.level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : next;
            for (int k = 0; k < BETWEEN; k++) {
                point(builder, matrix, view, position.lerp(end, k / (double)BETWEEN), size, TjeModConfig.cubeRed, TjeModConfig.cubeGreen,
                    TjeModConfig.cubeBlue, TjeModConfig.cubeAlpha);
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                point(builder, matrix, view, end, Math.min(size * 4.0F, 20.0F), 255, 0, 0, 60);
                break;
            }
            position = next;
            velocity = GauntLeap.step(velocity);
        }
        buffers.endBatch(ModUtils.BUFFS);
    }

    private static void point(VertexConsumer builder, PoseStack matrix, Vec3 view, Vec3 at, float size, int r, int g, int b, int a) {
        matrix.pushPose();
        matrix.translate(at.x - view.x, at.y - view.y, at.z - view.z);
        ModUtils.drawCubeFullLight(builder, matrix, 0.0F, 0.0F, 0.0F, size, size, 0.0F, 1.0F, 0.0F, 1.0F, r, g, b, a);
        matrix.popPose();
    }
}
