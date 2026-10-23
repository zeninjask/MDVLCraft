package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.gate.GateStatue;
import com.mdvlcraft.binder.gate.RuleGates;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Draws a trial gate's statue (RuleGates' Statues rule) as a plain humanoid in weathered stone (the zombie's model
 * geometry, its own texture from gen_gate_late.py). Registers itself on the mod bus, client only, so MDVLBinder needs
 * no line for it.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GateStatueRenderer extends HumanoidMobRenderer<GateStatue, HumanoidModel<GateStatue>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/entity/gate_statue.png");

    public GateStatueRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GateStatue statue) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(RegisterRenderers event) {
        {
            event.registerEntityRenderer(RuleGates.statue.get(), GateStatueRenderer::new);
        }
    }
}
