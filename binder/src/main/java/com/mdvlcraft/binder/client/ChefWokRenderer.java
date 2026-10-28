package com.mdvlcraft.binder.client;

import com.google.gson.JsonElement;
import com.mdvlcraft.binder.superchef.SuperChefSetup;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Frying Pan & Spatula in Epic Fight's third person: the pan in the right hand and the spatula in the left, as
 * Epic Fight draws a katana with its sheath (renderer mdvlcraft:chef_wok, named by assets/mdvlcraft/item_skins/chef_wok.json).
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ChefWokRenderer extends RenderItemBase {
    public ChefWokRenderer(JsonElement json) {
        super(json);
    }

    @SubscribeEvent
    public static void onRegisterItemRenderers(PatchedRenderersEvent.RegisterItemRenderer event) {
        event.addItemRenderer(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "chef_wok"), ChefWokRenderer::new);
    }

    @Override
    public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> patch, InteractionHand hand, OpenMatrix4f[] poses,
            MultiBufferSource buffers, PoseStack poseStack, int light, float partialTicks) {
        draw(new ItemStack(SuperChefSetup.CHEF_WOK_PAN.get()), patch, InteractionHand.MAIN_HAND, poses, buffers, poseStack, light);
        draw(new ItemStack(SuperChefSetup.CHEF_WOK_SPATULA.get()), patch, InteractionHand.OFF_HAND, poses, buffers, poseStack, light);
    }

    private void draw(ItemStack part, LivingEntityPatch<?> patch, InteractionHand hand, OpenMatrix4f[] poses, MultiBufferSource buffers,
            PoseStack poseStack, int light) {
        OpenMatrix4f transform = this.getCorrectionMatrix(patch, hand, poses);
        poseStack.pushPose();
        MathUtils.mulStack(poseStack, transform);
        itemRenderer.renderStatic(part, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY, poseStack, buffers, null, 0);
        poseStack.popPose();
    }
}
