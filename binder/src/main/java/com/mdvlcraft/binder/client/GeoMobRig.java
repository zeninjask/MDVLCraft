package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Epic Fight skeletons on GeckoLib mobs: the mages' patches (data/<mod>/epicfight_mobpatch) give them Epic Fight's biped
 * armature but no Epic Fight renderer, so they keep their own models. While Epic Fight plays an action on one (a hit, a
 * knockdown, Combat Evolution's execution or assassination), its pose goes onto the model's head, torso, arm and leg
 * bones and its root motion onto the whole model; otherwise the model plays its own animations.
 *
 * The joints become vanilla HumanoidModel part poses the way Epic Fight Compat Suite's ArmatureBinder poses vanilla
 * parts, and those go onto the bones the way GeckoLib's GeoArmorRenderer copies vanilla parts onto its armour bones.
 * mixin/geckolib/GeoEntityRendererMixin calls rootMotion once GeckoLib has turned the model to the mob's facing (every
 * pass, layers included) and bones once GeckoLib's own animations have run.
 */
public final class GeoMobRig {
    /** A bone, the joint it follows, and the vanilla part's rest offset and the bone's pivot (pixels). */
    private record Part(String bone, String joint, float restX, float restY, float pivotX, float pivotY) {
    }

    private static final Part[] PARTS = {
        new Part("head", "Head", 0.0F, 0.0F, 0.0F, 0.0F),
        new Part("torso", "Chest", 0.0F, 0.0F, 0.0F, 0.0F),
        new Part("right_arm", "Arm_R", -5.0F, 2.0F, -5.0F, 2.0F),
        new Part("left_arm", "Arm_L", 5.0F, 2.0F, 5.0F, 2.0F),
        new Part("right_leg", "Thigh_R", -1.9F, 12.0F, -2.0F, 12.0F),
        new Part("left_leg", "Thigh_L", 1.9F, 12.0F, 2.0F, 12.0F)
    };
    /** Vanilla model space: up 1.501 blocks, x and y flipped (LivingEntityRenderer's scale(-1, -1, 1)). */
    private static final OpenMatrix4f VANILLA_SPACE = vanillaSpace();
    private static final OpenMatrix4f FROM_VANILLA_SPACE = OpenMatrix4f.invert(VANILLA_SPACE, null);
    /** The pose of the mob being drawn, from rootMotion to bones; null when it isn't acting. */
    private static Entity posed;
    private static Armature armature;
    private static OpenMatrix4f[] poses;
    private static OpenMatrix4f rootDelta;

    private GeoMobRig() {
    }

    public static void rootMotion(Entity entity, PoseStack poseStack, float partialTick) {
        posed = null;
        LivingEntityPatch<?> patch = acting(entity);
        if (patch == null) {
            return;
        }

        armature = patch.getArmature();
        poses = armature.getPoseAsTransformMatrix(patch.<Animator>getAnimator().getPose(partialTick), false);
        Joint root = armature.searchJointByName("Root");
        rootDelta = OpenMatrix4f.mul(poses[root.getId()], root.getToOrigin(), null);
        // a joint scaled to nothing has no inverse (Epic Fight's invert crashes on it): leave such a pose undrawn
        if (rootDelta.determinant() == 0.0F) {
            return;
        }

        posed = entity;
        MathUtils.mulStack(poseStack, rootDelta);
    }

    public static void bones(Entity entity, BakedGeoModel model) {
        if (posed != entity) {
            return;
        }

        OpenMatrix4f toVanilla = OpenMatrix4f.mul(FROM_VANILLA_SPACE, OpenMatrix4f.invert(rootDelta, null), null);
        for (Part part : PARTS) {
            GeoBone bone = model.getBone(part.bone()).orElseThrow(() -> new IllegalStateException(
                "The model of " + ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) + " has no " + part.bone() + " bone for its Epic Fight skeleton"));
            Joint joint = armature.searchJointByName(part.joint());
            OpenMatrix4f m = OpenMatrix4f.mul(toVanilla, poses[joint.getId()], null);
            OpenMatrix4f.mul(m, joint.getToOrigin(), m);
            OpenMatrix4f.mul(m, VANILLA_SPACE, m);
            OpenMatrix4f.mul(m, new OpenMatrix4f().translate(part.restX() / 16.0F, part.restY() / 16.0F, 0.0F), m);
            float[] rotation = rotation(m);
            bone.setRotX(-rotation[0]);
            bone.setRotY(-rotation[1]);
            bone.setRotZ(rotation[2]);
            bone.setPosX(m.m30 * 16.0F - part.pivotX());
            bone.setPosY(part.pivotY() - m.m31 * 16.0F);
            bone.setPosZ(m.m32 * 16.0F);
        }
    }

    /** The Epic Fight patch of a mob drawn by GeckoLib in the middle of an action, or null. */
    private static LivingEntityPatch<?> acting(Entity entity) {
        if (!(entity instanceof LivingEntity living) || ClientEngine.getInstance().renderEngine.hasRendererFor(entity)) {
            return null;
        }

        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(living, LivingEntityPatch.class);
        return patch != null && patch.getArmature() instanceof HumanoidArmature && patch.getEntityState().inaction() ? patch : null;
    }

    private static OpenMatrix4f vanillaSpace() {
        OpenMatrix4f m = new OpenMatrix4f().translate(0.0F, 1.501F, 0.0F);
        m.m00 = -m.m00;
        m.m01 = -m.m01;
        m.m02 = -m.m02;
        m.m03 = -m.m03;
        m.m10 = -m.m10;
        m.m11 = -m.m11;
        m.m12 = -m.m12;
        m.m13 = -m.m13;
        return m;
    }

    /** A vanilla part's xRot, yRot and zRot (ModelPart turns z, then y, then x) from its matrix. */
    private static float[] rotation(OpenMatrix4f m) {
        float[] c0 = normalize(m.m00, m.m01, m.m02);
        float[] c1 = orthogonalize(m.m10, m.m11, m.m12, c0);
        float[] c2 = cross(c0, c1);
        float sinY = Math.max(-1.0F, Math.min(1.0F, -c0[2]));
        float yRot = (float)Math.asin(sinY);
        if (Math.abs(sinY) < 0.99999F) {
            return new float[]{(float)Math.atan2(c1[2], c2[2]), yRot, (float)Math.atan2(c0[1], c0[0])};
        }
        return new float[]{(float)Math.atan2(-c2[1], c1[1]), yRot, 0.0F};
    }

    private static float[] normalize(float x, float y, float z) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-6F ? new float[]{1.0F, 0.0F, 0.0F} : new float[]{x / length, y / length, z / length};
    }

    private static float[] orthogonalize(float x, float y, float z, float[] basis) {
        float dot = x * basis[0] + y * basis[1] + z * basis[2];
        return normalize(x - basis[0] * dot, y - basis[1] * dot, z - basis[2] * dot);
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
