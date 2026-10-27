package com.mdvlcraft.binder.client.irons;

import com.mdvlcraft.binder.mixin.irons.EnergySwirlLayerAccessor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.redspace.ironsspellbooks.render.ChargeSpellLayer;
import io.redspace.ironsspellbooks.render.EnergySwirlLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent.Modify;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/**
 * Iron's Spells draws its effects on a player as vanilla layers, which Epic Fight's renderer puts on the model's root in
 * the vanilla standing pose, so they don't follow Epic Fight's (and Epic Fight Extra's) animations. These patched layers
 * draw them on the animated model instead:
 * - the energy swirl (Charge, Evasion) over Epic Fight's own mesh, posed like the body under it;
 * - the projectile held while charging a spell (ChargeSpellLayer) in the posed right hand.
 */
public final class SpellEffectLayers {
    private SpellEffectLayers() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SpellEffectLayers::onModify);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onModify(Modify event) {
        PatchedLivingEntityRenderer renderer = (PatchedLivingEntityRenderer)event.get(EntityType.PLAYER);
        renderer.addPatchedLayerAlways(EnergySwirlLayer.Vanilla.class, new Swirl());
        renderer.addPatchedLayerAlways(ChargeSpellLayer.Vanilla.class, new HeldCharge());
    }

    private static final class Swirl
        extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
                                   RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            EnergySwirlLayerAccessor swirl = (EnergySwirlLayerAccessor)layer;
            if (!swirl.mdvlcraft$shouldRender().test(player)) {
                return;
            }
            float time = player.tickCount + partialTicks;
            ("slim".equals(player.getModelName()) ? Meshes.ALEX : Meshes.BIPED).get()
                .draw(poseStack, buffer, SwirlRenderType.swirl(swirl.mdvlcraft$texture(), time * 0.02F % 1.0F, time * 0.01F % 1.0F),
                      light, 0.8F, 0.8F, 0.8F, 1.0F, OverlayTexture.NO_OVERLAY, patch.getArmature(), poses);
        }
    }

    /** Runs Iron's layer with the right arm reset, from the posed arm joint, so translateToHand lands in the animated hand. */
    private static final class HeldCharge
        extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
                                   RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            PlayerModel<AbstractClientPlayer> model = layer.getParentModel();
            model.rightArm.setPos(0.0F, 0.0F, 0.0F);
            model.rightArm.setRotation(0.0F, 0.0F, 0.0F);
            int arm = patch.getArmature().searchJointByName("Arm_R").getId();
            OpenMatrix4f matrix = new OpenMatrix4f().scale(new Vec3f(-1.0F, -1.0F, 1.0F)).mulFront(poses[arm]);
            poseStack.pushPose();
            MathUtils.mulStack(poseStack, matrix);
            layer.render(poseStack, buffer, light, player, player.walkAnimation.position(), player.walkAnimation.speed(), partialTicks,
                         player.tickCount + partialTicks, yRot, xRot);
            poseStack.popPose();
        }
    }

    /** Vanilla's energy swirl (as on a charged creeper) with polygon offset, as it lies on Epic Fight's mesh instead of an inflated copy. */
    private static final class SwirlRenderType extends RenderType {
        private SwirlRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                                Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
        }

        static RenderType swirl(ResourceLocation texture, float u, float v) {
            return create("mdvlcraft_energy_swirl", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTexturingState(new OffsetTexturingStateShard(u, v))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false));
        }
    }
}
