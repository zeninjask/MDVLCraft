package com.mdvlcraft.binder.mixin.guard;

import com.mdvlcraft.binder.mixin.PoseStackAccessor;
import com.mdvlcraft.binder.util.CrashGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * An error in an entity's renderer is logged instead of crashing the client; the entity just isn't drawn that frame. The
 * pose stack is put back to where it was, as the renderer may have stopped part way.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderGuardMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), require = 0)
    private <E extends Entity> void mdvlcraft$guard(EntityRenderer<E> renderer, E entity, float yaw, float partialTick, PoseStack pose,
                                                    MultiBufferSource buffers, int light) {
        int depth = ((PoseStackAccessor) pose).mdvlcraft$poses().size();
        try {
            renderer.render(entity, yaw, partialTick, pose, buffers, light);
        } catch (Throwable error) {
            if (!CrashGuard.catchable(error)) {
                throw error;
            }
            while (((PoseStackAccessor) pose).mdvlcraft$poses().size() > depth) {
                pose.popPose();
            }
            CrashGuard.caught("entity render", CrashGuard.describe(entity) + ", renderer " + renderer.getClass().getName(), error);
        }
    }
}
