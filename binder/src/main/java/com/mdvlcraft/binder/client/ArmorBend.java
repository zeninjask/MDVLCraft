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
 * Bends GeckoLib armour (Fantasy Armor and the like) at the elbows and knees under Epic Fight. Epic Fight Compat Suite
 * draws such armour by posing the vanilla arm and leg parts rigidly on the upper arm and thigh joints, so with Epic
 * Fight Extra's animations, which bend elbows and knees a lot, sleeves, gauntlets, greaves and boots stay straight
 * while the body under them bends. Epic Fight's own armour is skinned and bends.
 *
 * While Compat Suite's GeoArmorLayer draws (begin/end), each limb gets the correction that moves what the upper joint
 * placed to where the lower joint (forearm, shin) would place it, in vanilla model space. GeckoLib quads on an arm,
 * leg or boot bone (mixin/geckolib/GeoArmorRendererMixin) are cut at the height of the lower joint (read from the
 * armature, so it matches Epic Fight Extra's proportions): the part above is drawn as before, the part below with the
 * correction, and the cut line halfway between so the two meet. Boots sit below the knee and follow the shin.
 */
public final class ArmorBend {
    public enum Limb {
        RIGHT_ARM("Arm_R", "Hand_R"),
        LEFT_ARM("Arm_L", "Hand_L"),
        RIGHT_LEG("Thigh_R", "Leg_R"),
        LEFT_LEG("Thigh_L", "Leg_L");

        final String upper;
        final String lower;

        Limb(String upper, String lower) {
            this.upper = upper;
            this.lower = lower;
        }
    }

    private static final Method ROOT_DELTA = binderMethod("rootDelta", Armature.class, OpenMatrix4f[].class);
    private static final Method VANILLA_MODEL_SPACE = binderMethod("vanillaModelSpace");
    private static final Matrix4f[] CORRECTION = new Matrix4f[Limb.values().length];
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

        OpenMatrix4f toVanilla = OpenMatrix4f.mul(
            OpenMatrix4f.invert((OpenMatrix4f)call(VANILLA_MODEL_SPACE), null), OpenMatrix4f.invert(rootDelta, null), null
        );
        OpenMatrix4f fromVanilla = OpenMatrix4f.invert(toVanilla, null);
        for (Limb each : Limb.values()) {
            Joint upper = armature.searchJointByName(each.upper);
            Joint lower = armature.searchJointByName(each.lower);
            if (upper == null || lower == null || poses[upper.getId()] == null || poses[lower.getId()] == null) {
                return;
            }

            OpenMatrix4f upperDelta = OpenMatrix4f.mul(poses[upper.getId()], upper.getToOrigin(), null);
            OpenMatrix4f lowerDelta = OpenMatrix4f.mul(poses[lower.getId()], lower.getToOrigin(), null);
            OpenMatrix4f correction = OpenMatrix4f.mul(toVanilla, lowerDelta, null);
            OpenMatrix4f.mul(correction, OpenMatrix4f.invert(upperDelta, null), correction);
            OpenMatrix4f.mul(correction, fromVanilla, correction);
            CORRECTION[each.ordinal()] = OpenMatrix4f.exportToMojangMatrix(correction);
            CUT[each.ordinal()] = OpenMatrix4f.invert(lower.getToOrigin(), null).m31;
        }

        active = true;
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
     * Draws a quad of the current limb. modelSpace is the pose GeckoLib's armour renderer started from (vanilla model
     * space), poseState the bone's pose as GeckoLib would draw it.
     */
    public static void quad(GeoQuad quad, Matrix4f poseState, Vector3f normal, Matrix4f modelSpace, VertexConsumer buffer,
                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        int i = limb.ordinal();
        float cut = CUT[i];
        Matrix4f bend = new Matrix4f(modelSpace).mul(CORRECTION[i]).mul(new Matrix4f(modelSpace).invert());
        Matrix4f lowerState = new Matrix4f(bend).mul(poseState);
        Vector3f lowerNormal = new Matrix3f(bend).transform(new Vector3f(normal)).normalize();
        List<Point> above = new ArrayList<>(5);
        List<Point> below = new ArrayList<>(5);
        GeoVertex[] vertices = quad.vertices();
        for (int k = 0; k < vertices.length; k++) {
            GeoVertex a = vertices[k];
            GeoVertex b = vertices[(k + 1) % vertices.length];
            boolean aAbove = a.position().y() >= cut;
            boolean bAbove = b.position().y() >= cut;
            (aAbove ? above : below).add(new Point(new Vector3f(a.position()), a.texU(), a.texV(), false));
            if (aAbove != bAbove) {
                float t = (cut - a.position().y()) / (b.position().y() - a.position().y());
                Point seam = new Point(
                    new Vector3f(a.position()).lerp(b.position(), t), a.texU() + (b.texU() - a.texU()) * t, a.texV() + (b.texV() - a.texV()) * t, true
                );
                above.add(seam);
                below.add(seam);
            }
        }

        polygon(above, poseState, poseState, lowerState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        polygon(below, lowerState, poseState, lowerState, lowerNormal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** A clipped polygon (3 to 5 points) as quads; points on the cut sit halfway between the two poses. */
    private static void polygon(List<Point> points, Matrix4f state, Matrix4f upperState, Matrix4f lowerState, Vector3f normal,
                                VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (points.size() < 3) {
            return;
        }

        int[][] quads = switch (points.size()) {
            case 3 -> new int[][]{{0, 1, 2, 2}};
            case 4 -> new int[][]{{0, 1, 2, 3}};
            case 5 -> new int[][]{{0, 1, 2, 3}, {0, 3, 4, 4}};
            default -> throw new IllegalStateException("A quad cut once has at most 5 corners, got " + points.size());
        };
        for (int[] corners : quads) {
            for (int corner : corners) {
                Point point = points.get(corner);
                Vector4f at;
                if (point.seam()) {
                    Vector4f up = upperState.transform(new Vector4f(point.position(), 1.0F));
                    at = lowerState.transform(new Vector4f(point.position(), 1.0F)).add(up).mul(0.5F);
                } else {
                    at = state.transform(new Vector4f(point.position(), 1.0F));
                }

                buffer.vertex(at.x(), at.y(), at.z(), red, green, blue, alpha, point.u(), point.v(), packedOverlay, packedLight,
                    normal.x(), normal.y(), normal.z());
            }
        }
    }

    private record Point(Vector3f position, float u, float v, boolean seam) {
    }
}
