package com.mdvlcraft.binder.mixin.efcompat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@Pseudo
@Mixin(targets = "com.efcompat.client.ArmatureBinder", remap = false)
public abstract class ArmatureBinderMixin {
    @Redirect(
        method = "bind",
        at = @At(
            value = "INVOKE",
            target = "Lyesman/epicfight/api/utils/math/OpenMatrix4f;invert(Lyesman/epicfight/api/utils/math/OpenMatrix4f;Lyesman/epicfight/api/utils/math/OpenMatrix4f;)Lyesman/epicfight/api/utils/math/OpenMatrix4f;"
        ),
        require = 0
    )
    private static OpenMatrix4f mdvlcraft$invertOrNull(OpenMatrix4f src, OpenMatrix4f dest) {
        return src.determinant() == 0.0F ? null : OpenMatrix4f.invert(src, dest);
    }
}
