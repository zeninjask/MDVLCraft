package com.mdvlcraft.binder.mixin.debug;

import com.mdvlcraft.binder.util.PacketStats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logs who changes an attribute named by -Dmdvlcraft.packettrace=attribute:<id> (util/PacketStats). */
@Mixin(AttributeInstance.class)
public abstract class AttributeTraceMixin {
    @Shadow
    public abstract Attribute getAttribute();

    @Inject(method = "setDirty", at = @At("HEAD"))
    private void mdvlcraft$trace(CallbackInfo ci) {
        if (PacketStats.TRACE != null) {
            PacketStats.traceAttribute(this.getAttribute());
        }
    }
}
