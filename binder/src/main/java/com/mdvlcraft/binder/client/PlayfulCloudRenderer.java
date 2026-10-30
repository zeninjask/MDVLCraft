package com.mdvlcraft.binder.client;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Playful Cloud in Epic Fight's third person (renderer mdvlcraft:playful_cloud, named by item_skins/playful_cloud.json):
 * an end section in each fist, gripped like Maki and Toji hold it (across the fist, upright and leaning a little
 * forward, a bit below its middle) and a middle section on chains between their lower ends. The middle one takes a
 * settled hanging shape, eased so it doesn't snap, and is kept in front of the body. tools/playful_cloud/preview/staff.js
 * is the same rules for the picker's previews.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PlayfulCloudRenderer extends RenderItemBase {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/item/playful_cloud_parts.png");
    private static final double SEC = 0.76, HALF = SEC / 2, CHAIN = 0.18, MAX_CHAIN = 0.3, LEAN = 0.35, LOW = 0.32;
    private static final double LET_GO = 1.15, FREE_RATE = 3.0;
    private static final double FIST = 0.27, BODY = 0.3, SIDE = 0.42, EASE = 0.06, HAND_EASE = 0.05;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    /** Where the eased parts were last frame. */
    private static final class State {
        Vec3 aR, aL, m0, m1;
        long last;
        /** How far the left section has swung out of the hand: 0 held, 1 in line with its chain. */
        double free;
    }

    public PlayfulCloudRenderer(JsonElement json) {
        super(json);
    }

    @SubscribeEvent
    public static void onRegisterItemRenderers(PatchedRenderersEvent.RegisterItemRenderer event) {
        event.addItemRenderer(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "playful_cloud"), PlayfulCloudRenderer::new);
    }

    @Override
    public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> patch, InteractionHand hand, OpenMatrix4f[] poses,
            MultiBufferSource buffers, PoseStack poseStack, int light, float partialTicks) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        Armature arm = patch.getArmature();
        LivingEntity entity = patch.getOriginal();
        State st = STATES.computeIfAbsent(entity, e -> new State());
        long now = System.nanoTime();
        double dt = st.last == 0 ? 1.0 : Math.min(0.1, (now - st.last) / 1e9);
        st.last = now;

        Vec3 root = at(arm, poses, "Root"), head = at(arm, poses, "Head");
        // up is the model's Y axis; Epic Fight's render space may have it flipped, so its sign follows head over hips
        Vec3 up = new Vec3(0, head.y < root.y ? -1 : 1, 0);
        Vec3 right = at(arm, poses, "Arm_R").subtract(at(arm, poses, "Arm_L"));
        right = right.subtract(up.scale(right.dot(up)));
        if (right.lengthSqr() < 1e-6) {
            return;
        }
        right = right.normalize();
        Vec3 fwd = up.cross(right).normalize();
        Vec3 low = root.subtract(up.scale(root.dot(up) - 0.05)), top = head.add(up.scale(0.3));

        // the held sections: [chain end, free end]
        Vec3 want = up.add(fwd.scale(LEAN));
        // the fist and forearm come from the forearm bone (Hand_*, which starts at the elbow), not from Tool_*: animations
        // move and turn the Tool joint to place their own weapon (the staff idle holds it out in front, the walk swings it)
        Vec3 fR = forearm(arm, poses, "Hand_R", fwd), fL = forearm(arm, poses, "Hand_L", fwd);
        Vec3 pR = at(arm, poses, "Hand_R").add(fR.scale(FIST)), pL = at(arm, poses, "Hand_L").add(fL.scale(FIST));
        Vec3 aR = grip(want, fwd, fR, st.aR);
        Vec3 aL = grip(want, fwd, fL, st.aL);
        // ease the grip so a hand that turns sharply doesn't make its section flick
        double hk = 1 - Math.exp(-dt / HAND_EASE);
        st.aR = st.aR == null ? aR : st.aR.add(aR.subtract(st.aR).scale(hk)).normalize();
        st.aL = st.aL == null ? aL : st.aL.add(aL.subtract(st.aL).scale(hk)).normalize();
        Vec3 P = pR.subtract(st.aR.scale(LOW)), Q = pL.subtract(st.aL.scale(LOW));

        // settled hang of the middle section between the chain ends
        double D = P.distanceTo(Q), span = SEC + 2 * CHAIN;
        Vec3 d = Q.subtract(P);
        d = d.lengthSqr() < 1e-6 ? right : d.normalize();
        double sag = Math.sqrt(Math.max(0, span * span - D * D)) * 0.42;
        Vec3 c = P.add(Q).scale(0.5).subtract(up.scale(sag));
        d = d.subtract(up.scale(0.35)).normalize();
        Vec3 e0 = c.subtract(d.scale(HALF)), e1 = c.add(d.scale(HALF));
        double k = 1 - Math.exp(-dt / EASE);
        st.m0 = st.m0 == null ? e0 : st.m0.add(e0.subtract(st.m0).scale(k));
        st.m1 = st.m1 == null ? e1 : st.m1.add(e1.subtract(st.m1).scale(k));
        Vec3 mid = st.m0.add(st.m1).scale(0.5), dir = st.m1.subtract(st.m0).normalize();
        Vec3 m0 = mid.subtract(dir.scale(HALF)), m1 = mid.add(dir.scale(HALF));
        if (m0.distanceTo(P) + m1.distanceTo(Q) > m0.distanceTo(Q) + m1.distanceTo(P)) {
            Vec3 t = m0; m0 = m1; m1 = t;
        }
        // the chains never stretch past MAX_CHAIN (the rod may give a little in length)
        m0 = tie(m0, P);
        m1 = tie(m1, Q);
        double len = m0.distanceTo(m1), keep = Math.min(SEC * 1.15, Math.max(SEC * 0.8, len));
        if (Math.abs(keep - len) > 1e-4) {
            Vec3 c2 = m0.add(m1).scale(0.5), u = len < 1e-4 ? right : m1.subtract(m0).normalize();
            m0 = c2.subtract(u.scale(keep / 2));
            m1 = c2.add(u.scale(keep / 2));
        }
        // never inside or behind the body: slide the rod forward until every point along it is clear
        double clear = 0;
        for (int i = 0; i <= 6; i++) {
            clear = Math.max(clear, intrusion(m0.lerp(m1, i / 6.0), low, top, right, fwd));
        }
        m0 = m0.add(fwd.scale(clear));
        m1 = m1.add(fwd.scale(clear));

        // A one-handed swing lets go of the left section (in an attack, the hands far apart, where a two-handed hold keeps them close):
        // it then swings out in line with its chain, and back into the hand when the hands meet again. FREE_RATE caps how
        // fast it goes either way, so a hand that is off the staff only briefly doesn't make it snap straight.
        double target = patch.getEntityState().attacking() && pL.distanceTo(pR) > LET_GO ? 1 : 0;
        st.free += Math.max(-FREE_RATE * dt, Math.min(FREE_RATE * dt, target - st.free));
        Vec3 lq = Q, le = Q.add(st.aL.scale(SEC));
        if (st.free > 0) {
            double f = st.free * st.free * (3 - 2 * st.free);
            Vec3 chain = Q.subtract(m1);
            chain = chain.lengthSqr() < 1e-6 ? m1.subtract(m0).normalize() : chain.normalize();
            Vec3 fq = m1.add(chain.scale(CHAIN));
            lq = Q.lerp(fq, f);
            Vec3 along = le.lerp(fq.add(chain.scale(SEC)), f).subtract(lq);
            le = lq.add((along.lengthSqr() < 1e-6 ? chain : along.normalize()).scale(SEC));
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        section(vc, pose, P, P.add(st.aR.scale(SEC)), light);
        section(vc, pose, lq, le, light);
        section(vc, pose, m0, m1, light);
        box(vc, pose, P, m0, 0.0175, STEEL, light);
        box(vc, pose, lq, m1, 0.0175, STEEL, light);
    }

    /** The section's direction: "want" square to the forearm; where the forearm lines up with it, the last direction (else forward) holds it steady. */
    private static Vec3 grip(Vec3 want, Vec3 fwd, Vec3 forearm, Vec3 last) {
        Vec3 a = want.subtract(forearm.scale(want.dot(forearm)));
        double l = a.length();
        if (l < 0.5) {
            Vec3 keep = last == null ? fwd : last;
            a = a.add(keep.subtract(forearm.scale(keep.dot(forearm))).scale((0.5 - l) * 2));
        }
        return a.lengthSqr() < 1e-8 ? (last == null ? fwd : last) : a.normalize();
    }

    /** Down the forearm bone: its local Y axis in the pose (the biped puts the fist FIST along it). */
    private static Vec3 forearm(Armature arm, OpenMatrix4f[] poses, String joint, Vec3 fallback) {
        OpenMatrix4f m = poses[arm.searchJointByName(joint).getId()];
        Vec3 v = new Vec3(m.m10, m.m11, m.m12);
        return v.lengthSqr() < 1e-8 ? fallback : v.normalize();
    }

    private static Vec3 tie(Vec3 m, Vec3 anchor) {
        Vec3 v = m.subtract(anchor);
        double l = v.length();
        return l > MAX_CHAIN ? anchor.add(v.scale(MAX_CHAIN / l)) : m;
    }

    /** How far a point is inside the body's column (feet to above the head), measured forward. */
    private static double intrusion(Vec3 x, Vec3 low, Vec3 top, Vec3 right, Vec3 fwd) {
        Vec3 ax = top.subtract(low);
        double t = Math.max(0, Math.min(1, x.subtract(low).dot(ax) / ax.lengthSqr()));
        Vec3 d = x.subtract(low.add(ax.scale(t)));
        return Math.abs(d.dot(right)) > SIDE ? 0 : Math.max(0, BODY - d.dot(fwd));
    }

    private static Vec3 at(Armature arm, OpenMatrix4f[] poses, String joint) {
        Vec3f v = poses[arm.searchJointByName(joint).getId()].toTranslationVector();
        return new Vec3(v.x, v.y, v.z);
    }

    // texture regions (u0, v0, u1, v1) of playful_cloud_parts.png
    private static final float[] WOOD = {0.0625F, 0.0625F, 0.4375F, 0.4375F};
    private static final float[] CAP = {0.5625F, 0.0625F, 0.9375F, 0.4375F};
    private static final float[] STEEL = {0.0625F, 0.5625F, 0.4375F, 0.9375F};

    /** One staff section from a to b: the red shaft with a black cap at each end. */
    private static void section(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, int light) {
        Vec3 d = b.subtract(a).normalize();
        box(vc, pose, a, b, 0.0425, WOOD, light);
        box(vc, pose, a, a.add(d.scale(0.06)), 0.0525, CAP, light);
        box(vc, pose, b.subtract(d.scale(0.06)), b, 0.0525, CAP, light);
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, double h, float[] uv, int light) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1e-8) {
            return;
        }
        d = d.normalize();
        Vec3 u = d.cross(Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(h);
        Vec3 v = d.cross(u).normalize().scale(h);
        Vec3[] side = {u, v, u.reverse(), v.reverse()};
        for (int i = 0; i < 4; i++) {
            Vec3 s = side[i], t = side[(i + 1) % 4];
            // a long face: centred on s, spanning t
            quad(vc, pose, a.add(s).subtract(t), b.add(s).subtract(t), b.add(s).add(t), a.add(s).add(t), s.normalize(), uv, light);
        }
        quad(vc, pose, b.add(u).add(v), b.add(u).subtract(v), b.subtract(u).subtract(v), b.subtract(u).add(v), d, uv, light);
        quad(vc, pose, a.add(u).add(v), a.subtract(u).add(v), a.subtract(u).subtract(v), a.add(u).subtract(v), d.reverse(), uv, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 n, float[] uv, int light) {
        Matrix4f m = pose.pose();
        Matrix3f nm = pose.normal();
        float[][] tex = {{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
        Vec3[] ps = {p0, p1, p2, p3};
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, (float)ps[i].x, (float)ps[i].y, (float)ps[i].z).color(255, 255, 255, 255).uv(tex[i][0], tex[i][1])
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, (float)n.x, (float)n.y, (float)n.z).endVertex();
        }
    }
}
