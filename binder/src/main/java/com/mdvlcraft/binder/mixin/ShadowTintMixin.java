package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.shadow.ShadowWeapons;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Shadow weapons (shadow/ShadowWeapons) are drawn pure black: every vertex colour the item's model (or its custom
 * renderer) writes is scaled down to near black, in hand, on the hotbar and in screens alike.
 */
@Mixin(ItemRenderer.class)
public abstract class ShadowTintMixin {
    private static final float SHADE = 0.06F;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource mdvlcraft$shadow(MultiBufferSource buffers, ItemStack stack, ItemDisplayContext context) {
        return ShadowWeapons.is(stack) ? type -> new Dark(buffers.getBuffer(type)) : buffers;
    }

    private static final class Dark extends VertexConsumerWrapper {
        Dark(VertexConsumer parent) {
            super(parent);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return super.color((int)(red * SHADE), (int)(green * SHADE), (int)(blue * SHADE), alpha);
        }
    }
}
