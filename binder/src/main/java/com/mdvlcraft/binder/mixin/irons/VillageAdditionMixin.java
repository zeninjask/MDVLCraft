package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.worldgen.VillageAddition;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iron's injects priest houses into vanilla village pools at server start; keep villages vanilla. */
@Mixin(value = VillageAddition.class, remap = false)
public abstract class VillageAdditionMixin {
    @Inject(method = "addNewVillageBuilding", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$skipPriestHouse(ServerAboutToStartEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
