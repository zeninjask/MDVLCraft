package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gaunt.GauntForm;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a player in Gaunt Vessel (gaunt/GauntForm). For the first SNAP_TICKS of entering it is the player's own body,
 * lifted off the ground while its bones snap one at a time at GauntForm.SNAPS (arms wrenched behind and over the head,
 * the neck twisted round, the legs broken, the spine jerked) and then stretched thin; then it is the Haunted Lord
 * (assets/mdvlcraft/haunted_lord/haunted_lord.json, from tools/haunted_lord/convert.py) unfolding out of it (transform).
 * Leaving plays all of it backwards. In the form: cast while casting, crawl while sneaking, sprint or walk while moving
 * (paced by the walk animation's distance), idle otherwise, each change blended over BLEND_TICKS; the head turns towards
 * where the player looks. Its own attacks, dodges, parries and executions (GauntClient.Action) play over all of these,
 * the execution facing its victim; mid-leap it holds the leap pose, guarding the block pose, and on a wall or ceiling it
 * crawls as it moves (paced by the distance crawled). Charging a leap (gaunt/GauntLeap) it crouches in the crawl with its hands down, pressing lower
 * and shaking; clinging to a wall or a ceiling it holds that pose turned onto the surface. The eyes glow (the glow mask,
 * drawn full bright).
 */
final class GauntRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation("mdvlcraft", "haunted_lord/haunted_lord.json");
    /** The model is about 3.7 blocks to the antler tips; drawn about 3.6 (1.3 times its first size). */
    private static final float SCALE = 0.975F;
    private static final float BLEND_TICKS = 5.0F;
    /** Animation seconds per unit of the walk animation's distance, per gait. */
    private static final float WALK_PACE = 0.06F;
    private static final float SPRINT_PACE = 0.08F;
    private static final float CRAWL_PACE = 0.055F;
    /** Animation seconds per block crawled on a wall or ceiling. */
    private static final float SURFACE_PACE = 3.0F;
    /** A spell begun in the form keeps the cast animation at least this long (seconds). */
    private static final float CAST_HOLD = 1.5F;
    /** The crawl's moment with both hands down: the pose held while clinging or charging a leap. */
    private static final float HANDS_PLANTED = 0.7F;
    private static NodeModel model;
    private static int head;
    private static final Map<Integer, NodeModel.Blend> BLENDS = new HashMap<>();

    private GauntRenderer() {
    }

    private static NodeModel model() {
        if (model == null) {
            model = NodeModel.load(MODEL);
            head = model.node("head");
        }
        return model;
    }

    static void render(Player player, Seen seen, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        PlayerRenderer renderer) {
        float now = player.level().getGameTime() + partialTick;
        float time = GauntForm.enteringTime(seen.phase, now - seen.since);
        GauntClient.Action action = GauntClient.action(player.getId());
        boolean acting = action != null && time >= GauntForm.ENTER_TICKS && (now - action.since) / 20.0F < model().length(action.animation);
        float yaw = acting && !Float.isNaN(action.yaw) ? action.yaw : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        if (time < GauntForm.SNAP_TICKS) {
            body((AbstractClientPlayer)player, time, now, yaw, poseStack, buffers, light, renderer);
            return;
        }
        NodeModel model = model();
        NodeModel.Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new NodeModel.Blend(BLEND_TICKS));
        float[][] pose;
        if (time < GauntForm.ENTER_TICKS) {
            pose = blend.pose(model, "transform", Math.min((time - GauntForm.SNAP_TICKS) / 20.0F, model.length("transform")), now);
        } else {
            Pick pick = pick(player, seen, partialTick, now);
            pose = blend.pose(model, pick.animation, pick.time, now);
        }
        GauntClient.Leap leap = GauntClient.leap(player.getId());
        Direction cling = time >= GauntForm.ENTER_TICKS && leap != null ? leap.cling : null;
        float charge = GauntClient.charge(player, partialTick);
        poseStack.pushPose();
        if (cling != null) {
            surface(poseStack, player, cling, partialTick, yaw);
        } else {
            if (time >= GauntForm.ENTER_TICKS && !acting) {
                look(pose, player, partialTick, yaw);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        }
        if (charge >= 0.0F) {
            // gathering itself: pressed lower and wider the longer it charges, shaking
            poseStack.mulPose(Axis.ZP.rotationDegrees(2.0F * charge * Mth.sin(now * 3.1F)));
            poseStack.scale(1.0F + 0.08F * charge, 1.0F - 0.2F * charge, 1.0F + 0.08F * charge);
        }
        poseStack.scale(SCALE, SCALE, SCALE);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        model.draw(pose, poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture)), light, overlay);
        if (model.glow != null) {
            model.draw(pose, poseStack, buffers.getBuffer(RenderType.eyes(model.glow)), 0xF000F0, overlay);
        }
        poseStack.popPose();
    }

    /** The animation shown in the form this frame, its time (seconds) and its length. */
    record Pick(String animation, float time, float length) {
    }

    static Pick pick(Player player, Seen seen, float partialTick, float now) {
        NodeModel model = model();
        GauntClient.Action action = GauntClient.action(player.getId());
        if (action != null && (now - action.since) / 20.0F < model.length(action.animation)) {
            return new Pick(action.animation, (now - action.since) / 20.0F, model.length(action.animation));
        }
        GauntClient.Leap leap = GauntClient.leap(player.getId());
        if (leap != null && leap.cling != null) {
            float length = model.length("crawl");
            return new Pick("crawl", (HANDS_PLANTED + leap.crawled * SURFACE_PACE) % length, length);
        }
        if (leap != null && leap.chargingSince >= 0) {
            return new Pick("crawl", HANDS_PLANTED, model.length("crawl"));
        }
        if (leap != null && leap.leaping && !player.onGround()) {
            float length = model.length("leap");
            return new Pick("leap", (now / 20.0F) % length, length);
        }
        if (leap != null && leap.blocking) {
            float length = model.length("block");
            return new Pick("block", (now / 20.0F) % length, length);
        }
        String animation;
        float time;
        float walked = player.walkAnimation.position(partialTick);
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        boolean moving = dx * dx + dz * dz > 1.0E-4;
        if (ClientMagicData.getSyncedSpellData(player).isCasting() || (now - seen.castAt) / 20.0F < CAST_HOLD) {
            animation = "cast";
            time = now / 20.0F;
        } else if (player.isCrouching()) {
            animation = "crawl";
            time = walked * CRAWL_PACE;
        } else if (moving && player.isSprinting()) {
            animation = "sprint";
            time = walked * SPRINT_PACE;
        } else if (moving) {
            animation = "walk";
            time = walked * WALK_PACE;
        } else {
            animation = "idle";
            time = now / 20.0F;
        }
        float length = model.length(animation);
        return new Pick(animation, time % length, length);
    }

    /**
     * Turns the model onto the surface it clings to (gaunt/GauntLeap): its ground is the block face the player's hitbox
     * touches, its feet towards the block, and it faces where the player looks along that face (up the wall, or the body's
     * way on a ceiling, when the look is straight into or out of it).
     */
    private static void surface(PoseStack poseStack, Player player, Direction face, float partialTick, float yaw) {
        Vector3f up = new Vector3f(-face.getStepX(), -face.getStepY(), -face.getStepZ());
        if (face == Direction.UP) {
            poseStack.translate(0.0F, player.getBbHeight(), 0.0F);
        } else {
            float half = player.getBbWidth() / 2.0F;
            poseStack.translate(face.getStepX() * half, player.getBbHeight() / 2.0F, face.getStepZ() * half);
        }
        Vec3 view = player.getViewVector(partialTick);
        Vector3f forward = new Vector3f((float)view.x, (float)view.y, (float)view.z);
        forward.sub(new Vector3f(up).mul(forward.dot(up)));
        if (forward.lengthSquared() < 0.04F) {
            forward = face == Direction.UP
                ? new Vector3f(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0.0F, Mth.cos(yaw * Mth.DEG_TO_RAD))
                : new Vector3f(0.0F, 1.0F, 0.0F);
        }
        forward.normalize();
        // the model faces -Z: columns are its x, y (the surface's normal) and z axes in the world
        Vector3f back = new Vector3f(forward).negate();
        Vector3f side = new Vector3f(up).cross(back);
        poseStack.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(side, up, back)));
    }

    /** Turns the head towards where the player looks (relative to the body, within limits). */
    private static void look(float[][] pose, Player player, float partialTick, float bodyYaw) {
        float turn = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot) - bodyYaw), -60.0F, 60.0F);
        float pitch = Mth.clamp(Mth.lerp(partialTick, player.xRotO, player.getXRot()), -35.0F, 35.0F);
        float[] trs = pose[head];
        Quaternionf q = new Quaternionf().rotateY(-turn * Mth.DEG_TO_RAD).rotateX(-pitch * Mth.DEG_TO_RAD)
            .mul(new Quaternionf(trs[3], trs[4], trs[5], trs[6]));
        trs[3] = q.x;
        trs[4] = q.y;
        trs[5] = q.z;
        trs[6] = q.w;
    }

    // ---- the player's own body breaking (entering time 0 .. SNAP_TICKS) ----

    /** A bone snapping `x` ticks ago: a near-instant crack to 1 with a small overshoot that rings out. */
    private static float snap(float x) {
        if (x <= 0.0F) {
            return 0.0F;
        }
        if (x < 1.4F) {
            float u = 1.0F - x / 1.4F;
            return 1.12F * (1.0F - u * u * u);
        }
        return 1.0F + 0.12F * (float)Math.exp(-(x - 1.4F) / 1.4F) * Mth.cos((x - 1.4F) * 2.2F);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** How far the body is lifted (blocks) at a point of the entering sequence. */
    static float lift(float time) {
        if (time >= GauntForm.SNAP_TICKS) {
            return 0.0F;
        }
        return 1.1F * smooth(time / 14.0F) - 0.25F * snap(time - 43.0F) * (1.0F - smooth((time - 50.0F) / 10.0F));
    }

    /** Snaps, by tick of GauntForm.SNAPS: the part and how far it is wrenched (radians, x y z). */
    private record Snap(int tick, int part, float x, float y, float z) {
    }

    private static final int HEAD = 0, RIGHT_ARM = 1, LEFT_ARM = 2, RIGHT_LEG = 3, LEFT_LEG = 4, SPINE = 5;
    private static final Snap[] BREAKS = {
        new Snap(12, RIGHT_ARM, 2.3F, 0.0F, 0.9F),       // wrenched up behind the back
        new Snap(19, LEFT_ARM, -2.6F, 1.1F, -0.5F),      // over the head, twisted
        new Snap(26, HEAD, -0.9F, 0.7F, 0.8F),           // thrown back and sideways
        new Snap(32, RIGHT_LEG, -1.5F, 0.0F, 0.35F),     // kicked up the wrong way
        new Snap(37, LEFT_LEG, 1.3F, -0.7F, -0.4F),      // bent back and turned in
        new Snap(43, SPINE, -0.55F, 0.0F, 0.2F),         // the spine arches back (pitch, -, roll)
        new Snap(47, HEAD, 0.0F, -1.9F, -0.6F),          // the neck twisted round
        new Snap(51, RIGHT_ARM, 0.6F, 0.8F, 0.5F),
        new Snap(51, SPINE, 0.0F, 0.0F, -0.35F),
        new Snap(55, LEFT_ARM, -0.5F, -0.9F, -0.6F),
        new Snap(55, LEFT_LEG, 0.4F, 0.0F, -0.3F),
        new Snap(58, HEAD, 0.5F, 0.4F, 0.9F),
        new Snap(58, RIGHT_LEG, 0.5F, 0.4F, 0.0F),
        new Snap(61, SPINE, 0.35F, 0.0F, 0.15F),
    };

    private static void body(AbstractClientPlayer player, float time, float now, float yaw, PoseStack poseStack, MultiBufferSource buffers,
        int light, PlayerRenderer renderer) {
        float rise = smooth(time / 14.0F);
        float[][] parts = new float[6][3];
        parts[HEAD][0] = -0.45F * rise;
        parts[RIGHT_ARM][2] = 0.3F * rise;
        parts[LEFT_ARM][2] = -0.3F * rise;
        parts[RIGHT_LEG][0] = 0.15F * rise;
        parts[LEFT_LEG][0] = -0.1F * rise;
        for (Snap snap : BREAKS) {
            float s = snap(time - snap.tick);
            parts[snap.part][0] += snap.x * s;
            parts[snap.part][1] += snap.y * s;
            parts[snap.part][2] += snap.z * s;
        }
        // trembling that grows as the body breaks
        float shake = 0.02F + 0.06F * time / GauntForm.SNAP_TICKS;
        for (int p = 0; p < 6; p++) {
            for (int c = 0; c < 3; c++) {
                parts[p][c] += shake * Mth.sin(now * (2.3F + 0.37F * p) + c * 2.1F + p);
            }
        }
        // the last snaps stretch the body thin and tall
        float stretch = 0.0F;
        for (int tick : new int[] {51, 55, 58, 61}) {
            stretch += 0.25F * snap(time - tick);
        }

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.riding = false;
        model.young = false;
        model.setupAnim(player, 0.0F, 0.0F, now, 0.0F, 0.0F);
        set(model.head, parts[HEAD]);
        set(model.rightArm, parts[RIGHT_ARM]);
        set(model.leftArm, parts[LEFT_ARM]);
        set(model.rightLeg, parts[RIGHT_LEG]);
        set(model.leftLeg, parts[LEFT_LEG]);
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);

        poseStack.pushPose();
        poseStack.translate(0.0F, lift(time), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.translate(0.0F, 1.2F, 0.0F);
        poseStack.mulPose(Axis.XP.rotation(parts[SPINE][0]));
        poseStack.mulPose(Axis.ZP.rotation(parts[SPINE][2]));
        poseStack.translate(0.0F, -1.2F, 0.0F);
        poseStack.scale(1.0F - 0.22F * stretch, 1.0F + 0.45F * stretch, 1.0F - 0.22F * stretch);
        poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())), light,
            OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static void set(ModelPart part, float[] rotation) {
        part.xRot = rotation[0];
        part.yRot = rotation[1];
        part.zRot = rotation[2];
    }
}
