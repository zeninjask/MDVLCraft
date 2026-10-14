package com.mdvlcraft.binder.mixin.efcompat;

import com.mdvlcraft.binder.client.ArmorBend;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** GeckoLib armour drawn by Compat Suite under Epic Fight bends at the elbows and knees (client/ArmorBend). */
@Pseudo
@Mixin(targets = "com.efcompat.client.layer.GeoArmorLayer", remap = false)
public abstract class GeoArmorLayerMixin {
    @Inject(method = "renderLayer", at = @At("HEAD"), require = 1)
    private void mdvlcraft$beginBend(LivingEntityPatch<?> patch, LivingEntity entity, RenderLayer<?, ?> layer, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot,
                                     float partialTicks, CallbackInfo ci) {
        ArmorBend.begin(patch.getArmature(), poses);
    }

    @Inject(method = "renderLayer", at = @At("RETURN"), require = 1)
    private void mdvlcraft$endBend(LivingEntityPatch<?> patch, LivingEntity entity, RenderLayer<?, ?> layer, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot,
                                   float partialTicks, CallbackInfo ci) {
        ArmorBend.end();
    }
}
