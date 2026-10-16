package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties.client.render.AlphaVertexConsumer;
import app.sedu.mc.parties.client.render.PartiesBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Parties draws party members as see-through ghosts through one shared consumer for every render type, so an enchanted
 * item's glint and item layers came back as the same consumer and VertexMultiConsumer threw "Duplicate delegates". Each
 * request gets its own consumer instead. The method is getBuffer, named as in the SRG-compiled jar.
 * Parties also routes the buffer to itself when it draws a ghost inside a ghost (an invisible party member rendered while
 * another is, e.g. a player under Smoke Bomb's invisibility), and getBuffer then called itself until the stack overflowed;
 * routing to itself keeps the source it had.
 */
@Mixin(value = PartiesBuffer.class, remap = false)
public abstract class PartiesBufferMixin {
    /** Parties' own ghost alpha (its shared consumer is built with 64). */
    @Unique
    private static final int mdvlcraft$GHOST_ALPHA = 64;

    @Shadow
    private MultiBufferSource activeSource;

    @Inject(method = "route", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noSelfRoute(MultiBufferSource source, CallbackInfo ci) {
        if (source == (Object)this) {
            ci.cancel();
        }
    }

    @Inject(method = "m_6299_", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$consumerPerRequest(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        AlphaVertexConsumer consumer = new AlphaVertexConsumer(mdvlcraft$GHOST_ALPHA);
        consumer.setDelegate(this.activeSource.getBuffer(type));
        cir.setReturnValue(consumer);
    }
}
