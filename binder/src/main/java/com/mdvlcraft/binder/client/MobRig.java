package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.integration.PassiveMobPatch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
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
 * Epic Fight skeletons on mobs that keep their own models: the mages' patches (data/<mod>/epicfight_mobpatch) give them
 * Epic Fight's biped armature but no Epic Fight renderer. While Epic Fight plays an action on one (a hit, a knockdown,
 * Combat Evolution's execution or assassination), its pose goes onto the model's head, body, arms and legs and its root
 * motion onto the whole model; otherwise the model plays its own animations.
 *
 * The joints become vanilla HumanoidModel part poses the way Epic Fight Compat Suite's ArmatureBinder poses vanilla
 * parts. A vanilla model (mixin/LivingEntityRendererMixin, after setupAnim) takes them as they are; a GeckoLib model
 * (mixin/geckolib/GeoEntityRendererMixin) takes them the way GeckoLib's GeoArmorRenderer copies vanilla parts onto its
 * armour bones: rootMotion once GeckoLib has turned the model to the mob's facing (every pass, layers included) and
 * bones once GeckoLib's own animations have run.
 *
 * Every hostile mob of other mods gets a PassiveMobPatch with the same biped armature (integration/EpicFightIntegration),
 * whatever its model's shape: its GeckoLib model takes the bones only when it has all six (under RENAMED's names), else
 * only the root motion. A mage patched in data (always a biped) must have all six.
 */
public final class MobRig {
    /** A GeckoLib bone and vanilla part, the joint they follow, the part's rest offset and the bone's pivot (pixels). */
    private record Part(String bone, Function<HumanoidModel<?>, ModelPart> part, String joint, float restX, float restY, float pivotX, float pivotY) {
    }

    private static final Part[] PARTS = {
        new Part("head", model -> model.head, "Head", 0.0F, 0.0F, 0.0F, 0.0F),
        new Part("torso", model -> model.body, "Chest", 0.0F, 0.0F, 0.0F, 0.0F),
        new Part("right_arm", model -> model.rightArm, "Arm_R", -5.0F, 2.0F, -5.0F, 2.0F),
        new Part("left_arm", model -> model.leftArm, "Arm_L", 5.0F, 2.0F, 5.0F, 2.0F),
        new Part("right_leg", model -> model.rightLeg, "Thigh_R", -1.9F, 12.0F, -2.0F, 12.0F),
        new Part("left_leg", model -> model.leftLeg, "Thigh_L", 1.9F, 12.0F, 2.0F, 12.0F)
    };
    /** GeckoLib models (by entity type) whose biped bones go by other names: a part's bone name -> the model's, matched by pivot. */
    private static final Map<String, Map<String, String>> RENAMED = Map.of(
        // its sides are swapped against the casting mobs' model (right_arm pivots at x 5, where their left_arm does) and "left _arm" has a space
        "iss_magicfromtheeast:jiangshi", Map.of("right_arm", "left _arm", "left_arm", "right_arm", "right_leg", "left_leg", "left_leg", "right_leg"));
    /** Vanilla model space: up 1.501 blocks, x and y flipped (LivingEntityRenderer's scale(-1, -1, 1)). */
    private static final OpenMatrix4f VANILLA_SPACE = vanillaSpace();
    private static final OpenMatrix4f FROM_VANILLA_SPACE = OpenMatrix4f.invert(VANILLA_SPACE, null);
    /** The pose of the mob being drawn, from rootMotion to bones; null when it isn't acting. */
    /** The entity posed last (weakly held: it must not keep a left world alive). */
    private static java.lang.ref.WeakReference<Entity> posed = new java.lang.ref.WeakReference<>(null);
    private static Armature armature;
    /** Whether the mob posed last has a PassiveMobPatch (its model may not be a biped). */
    private static boolean passive;
    private static OpenMatrix4f[] poses;
    private static OpenMatrix4f rootDelta;

    private MobRig() {
    }

    public static void rootMotion(Entity entity, PoseStack poseStack, float partialTick) {
        if (pose(entity, partialTick)) {
            MathUtils.mulStack(poseStack, rootDelta);
        }
    }

    public static void bones(Entity entity, BakedGeoModel model) {
        if (posed.get() != entity) {
            return;
        }

        String type = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        Map<String, String> names = RENAMED.getOrDefault(type, Map.of());
        GeoBone[] bones = new GeoBone[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) {
            String name = names.getOrDefault(PARTS[i].bone(), PARTS[i].bone());
            bones[i] = model.getBone(name).orElse(null);
            if (bones[i] == null) {
                if (passive) {
                    return;   // not a biped model: it keeps its own limb poses
                }
                throw new IllegalStateException("The model of " + type + " has no " + name + " bone for its Epic Fight skeleton");
            }
        }

        for (int i = 0; i < PARTS.length; i++) {
            Part part = PARTS[i];
            GeoBone bone = bones[i];
            float[] pose = partPose(part);
            bone.setRotX(-pose[3]);
            bone.setRotY(-pose[4]);
            bone.setRotZ(pose[5]);
            bone.setPosX(pose[0] - part.pivotX());
            bone.setPosY(part.pivotY() - pose[1]);
            bone.setPosZ(pose[2]);
        }
    }

    /** After a vanilla model's setupAnim, in vanilla model space. */
    public static void vanilla(LivingEntity entity, HumanoidModel<?> model, PoseStack poseStack, float partialTick) {
        if (!pose(entity, partialTick)) {
            return;
        }

        MathUtils.mulStack(poseStack, OpenMatrix4f.mul(OpenMatrix4f.mul(VANILLA_SPACE, rootDelta, null), FROM_VANILLA_SPACE, null));
        for (Part part : PARTS) {
            float[] pose = partPose(part);
            ModelPart modelPart = part.part().apply(model);
            modelPart.x = pose[0];
            modelPart.y = pose[1];
            modelPart.z = pose[2];
            modelPart.xRot = pose[3];
            modelPart.yRot = pose[4];
            modelPart.zRot = pose[5];
        }
        if (model instanceof PlayerModel<?> player) {
            player.hat.copyFrom(player.head);
            player.jacket.copyFrom(player.body);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }

    /** Takes the pose of a mob in the middle of an Epic Fight action; false when it isn't in one. */
    private static boolean pose(Entity entity, float partialTick) {
        posed = new java.lang.ref.WeakReference<>(null);
        LivingEntityPatch<?> patch = acting(entity);
        if (patch == null) {
            return false;
        }

        armature = patch.getArmature();
        passive = patch instanceof PassiveMobPatch;
        poses = armature.getPoseAsTransformMatrix(patch.<Animator>getAnimator().getPose(partialTick), false);
        Joint root = armature.searchJointByName("Root");
        rootDelta = OpenMatrix4f.mul(poses[root.getId()], root.getToOrigin(), null);
        // a joint scaled to nothing has no inverse (Epic Fight's invert crashes on it): leave such a pose undrawn
        if (rootDelta.determinant() == 0.0F) {
            return false;
        }

        posed = new java.lang.ref.WeakReference<>(entity);
        return true;
    }

    /** The patch of a mob Epic Fight doesn't draw itself, in the middle of an action, or null. */
    private static LivingEntityPatch<?> acting(Entity entity) {
        if (!(entity instanceof LivingEntity living) || ClientEngine.getInstance().renderEngine.hasRendererFor(entity)) {
            return null;
        }

        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(living, LivingEntityPatch.class);
        return patch != null && patch.getArmature() instanceof HumanoidArmature && patch.getEntityState().inaction() ? patch : null;
    }

    /** A part's vanilla pose under the current joints: x, y, z (pixels), xRot, yRot, zRot. */
    private static float[] partPose(Part part) {
        Joint joint = armature.searchJointByName(part.joint());
        OpenMatrix4f m = OpenMatrix4f.mul(OpenMatrix4f.mul(FROM_VANILLA_SPACE, OpenMatrix4f.invert(rootDelta, null), null), poses[joint.getId()], null);
        OpenMatrix4f.mul(m, joint.getToOrigin(), m);
        OpenMatrix4f.mul(m, VANILLA_SPACE, m);
        OpenMatrix4f.mul(m, new OpenMatrix4f().translate(part.restX() / 16.0F, part.restY() / 16.0F, 0.0F), m);
        float[] rotation = rotation(m);
        return new float[]{m.m30 * 16.0F, m.m31 * 16.0F, m.m32 * 16.0F, rotation[0], rotation[1], rotation[2]};
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
