package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.client.ForeignKeyMappings;
import cursedfate.init.CursedfateModKeyMappings;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static cursedfate.init.CursedfateModKeyMappings.*;

/**
 * Cursed Fate's keys run its procedures on the client as well as sending packets, so they are unbound and never
 * registered (no "Cursed Fate" category in Controls). Its techniques are cast from the binder's ability wheel.
 */
@Mixin(value = CursedfateModKeyMappings.class, remap = false)
public abstract class CursedfateModKeyMappingsMixin {
    @Inject(method = "registerKeyMappings", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$dropKeys(RegisterKeyMappingsEvent event, CallbackInfo ci) {
        ForeignKeyMappings.unbind(List.of(COMBAT_MODE, PRESET_SWICH, ABILITY_1, ABILITY_2, ABILITY_3, ABILITY_4, ABILITY_5, ABILITY_6,
                ABILITY_7, MENU, OUTPUT, ABILITY_USE_FOR_COMBAT_TYPE_2, QUICK_ABILITY_1, QUICK_ABILITY_2, QUICK_ABILITY_3, QUICK_ABILITY_4));
        ci.cancel();
    }
}
