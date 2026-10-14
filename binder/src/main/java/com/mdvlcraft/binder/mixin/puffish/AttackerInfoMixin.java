package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.content.SummonKills;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.puffish.skillsmod.util.AttackerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Summon kills credit nobody (SummonKills). */
@Mixin(value = AttackerInfo.class, remap = false)
public abstract class AttackerInfoMixin {
    @Inject(method = "detect", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noSummonCredit(Entity entity, Consumer<AttackerInfo> consumer, CallbackInfo ci) {
        if (SummonKills.creditBlocked()) {
            ci.cancel();
        }
    }
}
