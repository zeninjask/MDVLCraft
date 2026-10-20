package com.mdvlcraft.binder.mixin.geckolib;

import com.mdvlcraft.binder.client.MobRig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Puts an acting Epic Fight skeleton onto a GeckoLib mob's model (client/MobRig). */
@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class GeoEntityRendererMixin {
    private static final String ACTUALLY_RENDER = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;"
        + "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;"
        + "Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V";

    @Inject(method = ACTUALLY_RENDER, require = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;applyRotations(Lnet/minecraft/world/entity/Entity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"))
    private void mdvlcraft$rootMotion(PoseStack poseStack, Entity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                                      VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha, CallbackInfo ci) {
        MobRig.rootMotion(animatable, poseStack, partialTick);
    }

    @Inject(method = ACTUALLY_RENDER, require = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lsoftware/bernie/geckolib/model/GeoModel;handleAnimations(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;JLsoftware/bernie/geckolib/core/animation/AnimationState;)V"))
    private void mdvlcraft$bones(PoseStack poseStack, Entity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                                 VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                 float red, float green, float blue, float alpha, CallbackInfo ci) {
        MobRig.bones(animatable, model);
    }
}
