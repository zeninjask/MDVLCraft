package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

/**
 * Bends GeckoLib armour (Fantasy Armor and the like) under Epic Fight the way Epic Fight bends the player's skin, so the
 * skin doesn't show through. Epic Fight Compat Suite draws such armour by posing the vanilla parts rigidly: the body on
 * the Chest joint, the arms on the upper arm joints and the legs on the thigh joints. Epic Fight's skinned player
 * (assets/epicfight/animmodels/entity/biped.json) bends at the elbows and knees, and its torso blends from the Torso
 * (waist) joint at the hips to the Chest joint at the shoulders. With Epic Fight Extra's animations, which bend and
 * twist a lot, the skin showed at the elbows and knees and at the bottom of the torso and top of the legs.
 *
 * While Compat Suite's GeoArmorLayer draws (begin/end), each part gets corrections that move what its joint placed to
 * where another joint would place it, in vanilla model space. GeckoLib quads on an arm, leg, boot or body bone
 * (mixin/geckolib/GeoArmorRendererMixin) are cut into bands by height and each point placed like the skin there:
 * - Arms and legs: above the elbow or knee on the upper joint, below it on the lower one (forearm, shin), and the cut
 *   line on the Elbow or Knee joint, which the skin's ring there follows (halfway between, for an armature without
 *   them). Boots sit below the knee and follow the shin.
 * - Body (chestplate and the leggings' waist): blended between Chest and Torso with the skin's weights at that height,
 *   cut at the skin's rows so the armour bends where the skin does.
 * Joint heights are read from the armature, so they match Epic Fight Extra's proportions.
 */
public final class ArmorBend {
    public enum Limb {
        RIGHT_ARM("Arm_R", "Hand_R", "Elbow_R"),
        LEFT_ARM("Arm_L", "Hand_L", "Elbow_L"),
        RIGHT_LEG("Thigh_R", "Leg_R", "Knee_R"),
        LEFT_LEG("Thigh_L", "Leg_L", "Knee_L"),
        BODY("Chest", "Torso", null);

        final String upper;
        final String lower;
        final String seam;

        Limb(String upper, String lower, String seam) {
            this.upper = upper;
            this.lower = lower;
            this.seam = seam;
        }
    }

    /** The heights (blocks above the feet) of the skinned torso's vertex rows and their Chest weight (the rest is Torso). */
    private static final float[] BODY_ROWS = {0.75F, 0.875F, 1.0F, 1.125F, 1.25F, 1.375F, 1.5F};
    private static final float[] BODY_CHEST_WEIGHTS = {0.0F, 0.25F, 0.25F, 0.5F, 0.74F, 0.77F, 1.0F};
    private static final Method ROOT_DELTA = binderMethod("rootDelta", Armature.class, OpenMatrix4f[].class);
    private static final Method VANILLA_MODEL_SPACE = binderMethod("vanillaModelSpace");
    private static final Matrix4f[] CORRECTION = new Matrix4f[Limb.values().length];
    private static final Matrix4f[] SEAM_CORRECTION = new Matrix4f[Limb.values().length];
    /** The rest height of each lower joint above the feet, in blocks: GeckoLib armour's vertical coordinate. */
    private static final float[] CUT = new float[Limb.values().length];
    private static boolean active;
    private static Limb limb;

    private ArmorBend() {
    }

    /** Called as Compat Suite starts drawing an entity's GeckoLib armour; without the joints nothing bends. */
    public static void begin(Armature armature, OpenMatrix4f[] poses) {
        active = false;
        limb = null;
        OpenMatrix4f rootDelta = (OpenMatrix4f)call(ROOT_DELTA, armature, poses);
        if (rootDelta == null) {
            return;
        }

        OpenMatrix4f vanillaSpace = (OpenMatrix4f)call(VANILLA_MODEL_SPACE);
        if (singular(vanillaSpace) || singular(rootDelta)) {
            return;
        }
        OpenMatrix4f toVanilla = OpenMatrix4f.mul(OpenMatrix4f.invert(vanillaSpace, null), OpenMatrix4f.invert(rootDelta, null), null);
        if (singular(toVanilla)) {
            return;
        }
        OpenMatrix4f fromVanilla = OpenMatrix4f.invert(toVanilla, null);
        for (Limb each : Limb.values()) {
            Joint upper = armature.searchJointByName(each.upper);
            Joint lower = armature.searchJointByName(each.lower);
            Joint seam = each.seam == null ? null : armature.searchJointByName(each.seam);
            if (!posed(upper, poses) || !posed(lower, poses)) {
                return;
            }

            OpenMatrix4f upperPose = OpenMatrix4f.mul(poses[upper.getId()], upper.getToOrigin(), null);
            if (singular(upperPose) || singular(lower.getToOrigin())) {
                return;
            }
            OpenMatrix4f fromUpper = OpenMatrix4f.invert(upperPose, null);
            CORRECTION[each.ordinal()] = correction(toVanilla, lower, poses, fromUpper, fromVanilla);
            SEAM_CORRECTION[each.ordinal()] = posed(seam, poses) ? correction(toVanilla, seam, poses, fromUpper, fromVanilla) : null;
            CUT[each.ordinal()] = OpenMatrix4f.invert(lower.getToOrigin(), null).m31;
        }

        active = true;
    }

    /**
     * A matrix with no inverse: a joint scaled to nothing (the Mimic poses some that way). Epic Fight's invert writes into
     * its null destination for these and crashes, so such an entity's armour is drawn unbent.
     */
    private static boolean singular(OpenMatrix4f matrix) {
        return matrix.determinant() == 0.0F;
    }

    private static boolean posed(Joint joint, OpenMatrix4f[] poses) {
        return joint != null && poses[joint.getId()] != null;
    }

    /** Moves what the upper joint placed to where the given joint would place it, in vanilla model space. */
    private static Matrix4f correction(OpenMatrix4f toVanilla, Joint joint, OpenMatrix4f[] poses, OpenMatrix4f fromUpper, OpenMatrix4f fromVanilla) {
        OpenMatrix4f correction = OpenMatrix4f.mul(toVanilla, OpenMatrix4f.mul(poses[joint.getId()], joint.getToOrigin(), null), null);
        OpenMatrix4f.mul(correction, fromUpper, correction);
        OpenMatrix4f.mul(correction, fromVanilla, correction);
        return OpenMatrix4f.exportToMojangMatrix(correction);
    }

    /** Compat Suite's own anchors (private in its ArmatureBinder), so the bend works in the same space as its binding. */
    private static Method binderMethod(String name, Class<?>... parameters) {
        try {
            Method method = Class.forName("com.efcompat.client.ArmatureBinder").getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Epic Fight Compat Suite's ArmatureBinder." + name + " is gone", e);
        }
    }

    private static Object call(Method method, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Epic Fight Compat Suite's ArmatureBinder." + method.getName() + " failed", e);
        }
    }

    public static void end() {
        active = false;
        limb = null;
    }

    public static boolean active() {
        return active;
    }

    public static Limb limb() {
        return limb;
    }

    public static void enter(Limb entered) {
        limb = entered;
    }

    /**
     * Draws a quad of the current part. modelSpace is the pose GeckoLib's armour renderer started from (vanilla model
     * space), poseState the bone's pose as GeckoLib would draw it.
     */
    public static void quad(GeoQuad quad, Matrix4f poseState, Vector3f normal, Matrix4f modelSpace, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        int i = limb.ordinal();
        Matrix4f lowerState = conjugate(CORRECTION[i], modelSpace).mul(poseState);
        Matrix4f seamState = SEAM_CORRECTION[i] == null ? null : conjugate(SEAM_CORRECTION[i], modelSpace).mul(poseState);
        Vector3f lowerNormal = new Matrix3f(lowerState).mul(new Matrix3f(poseState).invert()).transform(new Vector3f(normal)).normalize();
        float[] cuts = limb == Limb.BODY ? BODY_ROWS : new float[]{CUT[i]};
        List<Point> polygon = new ArrayList<>(4);
        for (GeoVertex vertex : quad.vertices()) {
            polygon.add(new Point(new Vector3f(vertex.position()), vertex.texU(), vertex.texV()));
        }

        // bands from the bottom up: below the first cut, between each pair, above the last
        for (int band = 0; band <= cuts.length; band++) {
            List<Point> points = polygon;
            if (band > 0) {
                points = clip(points, cuts[band - 1], true);
            }
            if (band < cuts.length) {
                points = clip(points, cuts[band], false);
            }
            if (points.size() < 3) {
                continue;
            }

            float lowerWeight = limb == Limb.BODY ? 1.0F - chestWeight(middle(points)) : band == 0 ? 1.0F : 0.0F;
            Vector3f bandNormal = new Vector3f(normal).lerp(lowerNormal, lowerWeight).normalize();
            emit(points, cuts, poseState, lowerState, seamState, bandNormal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static Matrix4f conjugate(Matrix4f correction, Matrix4f modelSpace) {
        return new Matrix4f(modelSpace).mul(correction).mul(new Matrix4f(modelSpace).invert());
    }

    /** The part of a convex polygon above (or below) a height, with points added where its edges cross it. */
    private static List<Point> clip(List<Point> points, float height, boolean keepAbove) {
        List<Point> kept = new ArrayList<>(points.size() + 2);
        for (int k = 0; k < points.size(); k++) {
            Point a = points.get(k);
            Point b = points.get((k + 1) % points.size());
            boolean aIn = keepAbove ? a.position().y() >= height : a.position().y() <= height;
            boolean bIn = keepAbove ? b.position().y() >= height : b.position().y() <= height;
            if (aIn) {
                kept.add(a);
            }
            if (aIn != bIn) {
                float t = (height - a.position().y()) / (b.position().y() - a.position().y());
                kept.add(new Point(new Vector3f(a.position()).lerp(b.position(), t), a.u() + (b.u() - a.u()) * t, a.v() + (b.v() - a.v()) * t));
            }
        }
        return kept;
    }

    private static float middle(List<Point> points) {
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        for (Point point : points) {
            low = Math.min(low, point.position().y());
            high = Math.max(high, point.position().y());
        }
        return (low + high) / 2.0F;
    }

    /** The skinned torso's Chest weight at a height, between its rows; below the hips all Torso, above the shoulders all Chest. */
    private static float chestWeight(float height) {
        if (height <= BODY_ROWS[0]) {
            return BODY_CHEST_WEIGHTS[0];
        }
        for (int k = 1; k < BODY_ROWS.length; k++) {
            if (height <= BODY_ROWS[k]) {
                float t = (height - BODY_ROWS[k - 1]) / (BODY_ROWS[k] - BODY_ROWS[k - 1]);
                return BODY_CHEST_WEIGHTS[k - 1] + (BODY_CHEST_WEIGHTS[k] - BODY_CHEST_WEIGHTS[k - 1]) * t;
            }
        }
        return BODY_CHEST_WEIGHTS[BODY_CHEST_WEIGHTS.length - 1];
    }

    /** Where a point goes: limbs by which side of the cut it is on (on it: the Elbow or Knee joint), the body blended. */
    private static Vector4f place(Point point, float[] cuts, Matrix4f upperState, Matrix4f lowerState, Matrix4f seamState) {
        Vector4f position = new Vector4f(point.position(), 1.0F);
        float y = point.position().y();
        if (limb == Limb.BODY) {
            Vector4f chest = upperState.transform(new Vector4f(position));
            return lowerState.transform(new Vector4f(position)).lerp(chest, chestWeight(y));
        }
        if (Math.abs(y - cuts[0]) < 1.0E-5F) {
            // an armature without Elbow or Knee joints: halfway between the two sides
            return seamState != null
                ? seamState.transform(position)
                : upperState.transform(new Vector4f(position)).lerp(lowerState.transform(new Vector4f(position)), 0.5F);
        }
        return (y > cuts[0] ? upperState : lowerState).transform(position);
    }

    /** A clipped convex polygon as a fan of quads (a triangle repeats its last corner). */
    private static void emit(List<Point> points, float[] cuts, Matrix4f upperState, Matrix4f lowerState, Matrix4f seamState, Vector3f normal,
                             VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        for (int k = 1; k + 1 < points.size(); k += 2) {
            int[] corners = {0, k, k + 1, Math.min(k + 2, points.size() - 1)};
            for (int corner : corners) {
                Point point = points.get(corner);
                Vector4f at = place(point, cuts, upperState, lowerState, seamState);
                buffer.vertex(at.x(), at.y(), at.z(), red, green, blue, alpha, point.u(), point.v(), packedOverlay, packedLight,
                    normal.x(), normal.y(), normal.z());
            }
        }
    }

    private record Point(Vector3f position, float u, float v) {
    }
}
