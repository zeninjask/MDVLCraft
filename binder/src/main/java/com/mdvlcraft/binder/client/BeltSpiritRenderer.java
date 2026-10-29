package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.content.LanternBelt;
import com.mojang.blaze3d.vertex.PoseStack;
import cursedfate.client.model.Modeltojisworm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.client.render.CuriosLayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.compat.CuriosCompat.EpicFightCurioRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Inventory Cursed Spirit worn on the belt (content/LanternBelt) is drawn as Cursed Fate draws it from its own
 * accessory slot: Toji's worm coiled round the body (Modeltojisworm's body part). On the vanilla model it follows the body;
 * under Epic Fight it follows the armature's JOINT, the way Epic Fight places its own curios.
 */
public final class BeltSpiritRenderer implements ICurioRenderer, EpicFightCurioRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("cursedfate", "textures/entities/tojisworm.png");
    private static final String JOINT = "Chest";
    /** From the Epic Fight joint to where the vanilla body's pivot (the neck) would be, in blocks (y points down). */
    private static final float JOINT_Y = -0.4F;
    private static ModelPart worm;

    static void register() {
        var item = ForgeRegistries.ITEMS.getValue(LanternBelt.SPIRIT);
        if (item != null) {
            CuriosRendererRegistry.register(item, BeltSpiritRenderer::new);
        }
    }

    private static ModelPart worm() {
        if (worm == null) {
            worm = new Modeltojisworm<>(Minecraft.getInstance().getEntityModels().bakeLayer(Modeltojisworm.LAYER_LOCATION)).Body;
        }
        return worm;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (parent.getModel() instanceof HumanoidModel<?> model) {
            worm().copyFrom(model.body);
            draw(slotContext.entity(), poseStack, buffer, light);
        }
    }

    @Override
    public void draw(ItemStack stack, SlotContext slotContext, LivingEntityPatch<LivingEntity> patch, LivingEntity entity,
            CuriosLayer<LivingEntity, EntityModel<LivingEntity>> layer, PoseStack poseStack, MultiBufferSource buffer, int light,
            OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
        Joint joint = patch.getArmature().searchJointByName(JOINT);
        if (joint == null) {
            return;
        }
        poseStack.pushPose();
        MathUtils.mulStack(poseStack, new OpenMatrix4f().scale(new Vec3f(-1.0F, -1.0F, 1.0F)).mulFront(poses[joint.getId()]));
        poseStack.translate(0.0F, JOINT_Y, 0.0F);
        worm().setPos(0.0F, 0.0F, 0.0F);
        worm().setRotation(0.0F, 0.0F, 0.0F);
        draw(entity, poseStack, buffer, light);
        poseStack.popPose();
    }

    private static void draw(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int light) {
        worm().render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F));
    }
}
