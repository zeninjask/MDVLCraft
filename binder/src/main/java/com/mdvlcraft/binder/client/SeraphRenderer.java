package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.seraph.SeraphForm;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a player in Seraph Embodiment as the Seraphim (assets/mdvlcraft/seraph/seraph.json, from tools/seraph/convert.py):
 * the glTF nodes posed by their own keyframes, at SeraphForm.SCALE, with the eye centred on the eye-sized hitbox.
 * Entering plays spawn, leaving plays it backwards; in the form a spell plays pose (and keeps it while casting), moving
 * plays fly and keeping still plays idle, each change blended over BLEND_TICKS.
 */
final class SeraphRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation("mdvlcraft", "seraph/seraph.json");
    private static final float SPAWN_SPEED = 4.5F * 20.0F / SeraphForm.SPAWN_TICKS;
    /** A spell's pose (4 s) plays twice as fast. */
    private static final float POSE_SPEED = 2.0F;
    private static final float BLEND_TICKS = 5.0F;
    private static NodeModel model;
    private static float[] eye;
    private static final Map<Integer, NodeModel.Blend> BLENDS = new HashMap<>();

    private SeraphRenderer() {
    }

    private static NodeModel model() {
        if (model == null) {
            model = NodeModel.load(MODEL);
            eye = NodeModel.floats(model.json.getAsJsonArray("eye_center"));
        }
        return model;
    }

    static void render(Player player, Seen seen, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        NodeModel model = model();
        float now = player.level().getGameTime() + partialTick;
        NodeModel.Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new NodeModel.Blend(BLEND_TICKS));
        float[][] pose = choose(model, player, seen, now, blend);

        poseStack.pushPose();
        poseStack.translate(0.0F, SeraphForm.EYE / 2.0F, 0.0F);
        // The model's eyes look along -Z (its pupil is on the eye's -Z face); turn them the way the player looks.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot)));
        poseStack.scale(SeraphForm.SCALE, SeraphForm.SCALE, SeraphForm.SCALE);
        poseStack.translate(-eye[0], -eye[1], -eye[2]);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        model.draw(pose, poseStack, consumer, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        poseStack.popPose();
    }

    /** Picks the animation and its time (seconds) for this frame and returns the (blended) pose. */
    private static float[][] choose(NodeModel model, Player player, Seen seen, float now, NodeModel.Blend blend) {
        float age = (now - seen.since) / 20.0F;
        float spawn = model.length("spawn");
        String animation;
        float time;
        switch (seen.phase) {
            case ENTERING -> {
                animation = "spawn";
                time = Math.min(age * SPAWN_SPEED, spawn);
            }
            case EXITING -> {
                animation = "spawn";
                time = Math.max(spawn - age * SPAWN_SPEED, 0.0F);
            }
            default -> {
                float pose = model.length("pose") / POSE_SPEED;
                if (ClientMagicData.getSyncedSpellData(player).isCasting() && (now - seen.castAt) / 20.0F >= pose) {
                    seen.castAt = (long)now;
                }
                float sinceCast = (now - seen.castAt) / 20.0F;
                double dx = player.getX() - player.xo;
                double dy = player.getY() - player.yo;
                double dz = player.getZ() - player.zo;
                if (sinceCast < pose) {
                    animation = "pose";
                    time = sinceCast * POSE_SPEED;
                } else if (dx * dx + dy * dy + dz * dz > 4.0E-4) {
                    animation = "fly";
                    time = now / 20.0F % model.length("fly");
                } else {
                    animation = "idle";
                    time = now / 20.0F % model.length("idle");
                }
            }
        }
        return blend.pose(model, animation, time, now);
    }
}
