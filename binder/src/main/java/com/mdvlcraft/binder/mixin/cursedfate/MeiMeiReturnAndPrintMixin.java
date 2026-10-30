package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.compat.CursedFateVars;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mei Mei's per-tick copy reads Cursed Fate's Technique, InnateMenuOpened, AbilityToCast and RerollCustomCT by running
 * `/cursedfate Datapack ReturnAndPrint @s <name>` (a command parse, an entity selector and a reflective field lookup) for
 * every player every tick; on the server CursedFateVars answers it directly.
 */
@Mixin(targets = {
    "com.cursedfate_roslon_meimei.procedures.PlayerTicksProcedure$1",
    "com.cursedfate_roslon_meimei.procedures.PlayerTicksProcedure$2",
    "com.cursedfate_roslon_meimei.procedures.PlayerTicksProcedure$4",
    "com.cursedfate_roslon_meimei.procedures.PlayerTicksProcedure$5"
}, remap = false)
public abstract class MeiMeiReturnAndPrintMixin {
    @Inject(method = "getResult", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$direct(Entity entity, String command, CallbackInfoReturnable<String> cir) {
        String result = CursedFateVars.returnAndPrint(entity, command);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
