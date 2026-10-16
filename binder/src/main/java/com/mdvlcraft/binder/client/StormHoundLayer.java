package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

/**
 * The charged creeper's energy swirl round the Storm Hounds' wolves (modified/SummonSpells.StormHounds), on a wolf model
 * inflated by half a pixel. The client knows a Storm Hound by its summon name, which the server syncs.
 */
public class StormHoundLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    public static final ModelLayerLocation ARMOR = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "storm_hound"), "armor");
    private static final ResourceLocation SWIRL = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper_armor.png");
    private static final String NAME_KEY = "spell.mdvlcraft.storm_hounds";
    private final WolfModel<Wolf> model;

    public StormHoundLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent, EntityModelSet models) {
        super(parent);
        this.model = new WolfModel<>(models.bakeLayer(ARMOR));
    }

    /** Vanilla's wolf mesh (WolfModel.createBodyLayer) with every cube grown by `inflate`. */
    public static LayerDefinition createArmorLayer() {
        CubeDeformation inflate = new CubeDeformation(0.5F);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
        head.addOrReplaceChild("real_head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, inflate)
            .texOffs(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, inflate)
            .texOffs(16, 14).addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, inflate)
            .texOffs(0, 10).addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, inflate), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, inflate),
            PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float)(Math.PI / 2), 0.0F, 0.0F));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, inflate),
            PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float)(Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, inflate);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(0.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(0.5F, 16.0F, -4.0F));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, (float)(Math.PI / 5), 0.0F, 0.0F));
        tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, inflate), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    static boolean isStormHound(Wolf wolf) {
        Component name = wolf.getCustomName();
        return name != null && name.getContents() instanceof TranslatableContents key && NAME_KEY.equals(key.getKey());
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Wolf wolf, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!isStormHound(wolf)) {
            return;
        }
        float time = wolf.tickCount + partialTick;
        this.model.prepareMobModel(wolf, limbSwing, limbSwingAmount, partialTick);
        this.getParentModel().copyPropertiesTo(this.model);
        VertexConsumer swirl = buffers.getBuffer(RenderType.energySwirl(SWIRL, time * 0.01F % 1.0F, time * 0.01F % 1.0F));
        this.model.setupAnim(wolf, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.model.renderToBuffer(pose, swirl, light, OverlayTexture.NO_OVERLAY, 0.5F, 0.5F, 0.5F, 1.0F);
    }
}
