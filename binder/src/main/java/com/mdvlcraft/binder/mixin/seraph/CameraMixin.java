package com.mdvlcraft.binder.mixin.seraph;

import com.mdvlcraft.binder.seraph.SeraphForm;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seraph Embodiment (seraph/SeraphForm): in first person and the third person view from behind, the view sits at the
 * centre of the seraph's eye, moved just out of the eye's front face (the way the eye looks), so neither the eye nor the
 * wings block it. The front view (mirrored) stays where it is, looking back at the seraph.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    /** How far in front of the eye's front face the camera sits. */
    private static final double MDVLCRAFT_IN_FRONT = 0.15;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void mdvlcraft$seraphEye(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (!mirrored && entity instanceof Player player && SeraphForm.CLIENT.containsKey(player.getId())) {
            Vec3 facing = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick));
            this.setPosition(player.getEyePosition(partialTick).add(facing.scale(SeraphForm.EYE / 2.0F + MDVLCRAFT_IN_FRONT)));
        }
    }
}
