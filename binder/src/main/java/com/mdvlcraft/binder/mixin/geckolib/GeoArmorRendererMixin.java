package com.mdvlcraft.binder.mixin.geckolib;

import com.mdvlcraft.binder.client.ArmorBend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Fantasy Armor's capes (every bone named cape, with what hangs off it) are not drawn.
 *
 * Marks which part (body, arm, leg or boot bone and their children; an arm or leg inside the body counts as the limb)
 * GeckoLib is drawing and, while client/ArmorBend is on, draws that part's quads bent like Epic Fight's skin. Everything
 * else (the helmet) is drawn as GeckoLib's own createVerticesOfQuad does.
 */
@Mixin(value = GeoArmorRenderer.class, remap = false)
public abstract class GeoArmorRendererMixin {
    @Shadow
    protected GeoBone body;
    @Shadow
    protected GeoBone rightArm;
    @Shadow
    protected GeoBone leftArm;
    @Shadow
    protected GeoBone rightLeg;
    @Shadow
    protected GeoBone leftLeg;
    @Shadow
    protected GeoBone rightBoot;
    @Shadow
    protected GeoBone leftBoot;
    @Shadow
    protected Matrix4f entityRenderTranslations;
    @Unique
    private GeoBone mdvlcraft$limbBone;
    @Unique
    private GeoBone mdvlcraft$bodyBone;

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
        at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$noFantasyCape(PoseStack poseStack, Item animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                         VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                         float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (bone.getName().toLowerCase(Locale.ROOT).contains("cape") && "fantasy_armor".equals(ForgeRegistries.ITEMS.getKey(animatable).getNamespace())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
        at = @At("HEAD"), require = 1)
    private void mdvlcraft$enterLimb(PoseStack poseStack, Item animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                     VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                     float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!ArmorBend.active() || this.mdvlcraft$limbBone != null) {
            return;
        }

        if (bone == this.body && this.mdvlcraft$bodyBone == null) {
            ArmorBend.enter(ArmorBend.Limb.BODY);
            this.mdvlcraft$bodyBone = bone;
            return;
        }

        ArmorBend.Limb limb = bone == this.rightArm ? ArmorBend.Limb.RIGHT_ARM
            : bone == this.leftArm ? ArmorBend.Limb.LEFT_ARM
            : bone == this.rightLeg || bone == this.rightBoot ? ArmorBend.Limb.RIGHT_LEG
            : bone == this.leftLeg || bone == this.leftBoot ? ArmorBend.Limb.LEFT_LEG
            : null;
        if (limb != null) {
            ArmorBend.enter(limb);
            this.mdvlcraft$limbBone = bone;
        }
    }

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
        at = @At("RETURN"), require = 1)
    private void mdvlcraft$leaveLimb(PoseStack poseStack, Item animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                     VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                     float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (bone == this.mdvlcraft$limbBone) {
            ArmorBend.enter(this.mdvlcraft$bodyBone != null ? ArmorBend.Limb.BODY : null);
            this.mdvlcraft$limbBone = null;
        } else if (bone == this.mdvlcraft$bodyBone) {
            ArmorBend.enter(null);
            this.mdvlcraft$bodyBone = null;
        }
    }

    /** Overrides GeoRenderer's default; the plain branch is its body. */
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay,
                                     float red, float green, float blue, float alpha) {
        if (ArmorBend.active() && ArmorBend.limb() != null) {
            ArmorBend.quad(quad, poseState, normal, this.entityRenderTranslations, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            Vector4f at = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
            buffer.vertex(at.x(), at.y(), at.z(), red, green, blue, alpha, vertex.texU(), vertex.texV(), packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z());
        }
    }
}
