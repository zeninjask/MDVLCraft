package com.mdvlcraft.binder.client.antimagic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tiago.antimagic.capability.BlackAstaCapability;
import com.tiago.antimagic.capability.BlackAstaCapabilityProvider;
import com.tiago.antimagic.client.renderer.DemonicAssimilationLayer;
import com.tiago.antimagic.client.renderer.DemonicSkinLayer;
import com.tiago.antimagic.client.renderer.HornLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent.Modify;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.api.client.model.Meshes;

/**
 * Anti-Magic Unleashed draws Demonic Assimilation (dark skin, feathered wings, horns) as vanilla player layers, which Epic
 * Fight's player renderer leaves out. These patched layers draw them on the animated model: the skin over Epic Fight's own
 * mesh, the wings on the chest joint and the horns on the head joint, the way Epic Fight places capes and helmets.
 */
public final class DemonicAssimilationLayers {
    private static final int CHEST = 8;
    private static final int HEAD = 9;

    private DemonicAssimilationLayers() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DemonicAssimilationLayers::onModify);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onModify(Modify event) {
        PatchedLivingEntityRenderer renderer = (PatchedLivingEntityRenderer)event.get(EntityType.PLAYER);
        renderer.addPatchedLayer(DemonicSkinLayer.class, new Skin());
        renderer.addPatchedLayer(DemonicAssimilationLayer.class, new Jointed(CHEST, -0.4F));
        renderer.addPatchedLayer(HornLayer.class, new Jointed(HEAD, 0.0F));
    }

    private static boolean active(AbstractClientPlayer player) {
        return player.getCapability(BlackAstaCapabilityProvider.BLACK_ASTA_CAPABILITY).map(BlackAstaCapability::isBlackAstaActive).orElse(false);
    }

    private static final class Skin
        extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
                                   RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            if (active(player)) {
                ("slim".equals(player.getModelName()) ? Meshes.ALEX : Meshes.BIPED).get()
                    .draw(poseStack, buffer, RenderType.entityTranslucent(DemonicSkinLayer.ASIMILACION_TEXTURE), light, 1.0F, 1.0F, 1.0F, 1.0F,
                          LivingEntityRenderer.getOverlayCoords(player, 0.0F), patch.getArmature(), poses);
            }
        }
    }

    /** Runs the vanilla layer with its model part reset, so the layer sits where the joint is posed. */
    private static final class Jointed
        extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
        private final int joint;
        private final float lift;

        Jointed(int joint, float lift) {
            this.joint = joint;
            this.lift = lift;
        }

        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
                                   RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer, PoseStack poseStack,
                                   MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
            if (!active(player)) {
                return;
            }

            PlayerModel<AbstractClientPlayer> model = layer.getParentModel();
            ModelPart part = this.joint == HEAD ? model.head : model.body;
            part.setPos(0.0F, 0.0F, 0.0F);
            part.setRotation(0.0F, 0.0F, 0.0F);
            OpenMatrix4f matrix = new OpenMatrix4f().scale(new Vec3f(-1.0F, -1.0F, 1.0F)).mulFront(poses[this.joint]);
            poseStack.pushPose();
            MathUtils.mulStack(poseStack, matrix);
            poseStack.translate(0.0F, this.lift, 0.0F);
            layer.render(poseStack, buffer, light, player, player.walkAnimation.position(), player.walkAnimation.speed(), partialTicks,
                         player.tickCount + partialTicks, yRot, xRot);
            poseStack.popPose();
        }
    }
}
