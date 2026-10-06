package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.client.ForeignKeyMappings;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Iron's cast, spell-wheel, spell-bar and quick-cast keys are unbound and never registered; spells are cast from the
 * binder's ability wheel. Iron's client tick keeps running because it also clears its right-click suppression after a cast.
 */
@Mixin(value = KeyMappings.class, remap = false)
public abstract class KeyMappingsMixin {
    @Inject(method = "onRegisterKeybinds", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$dropKeys(RegisterKeyMappingsEvent event, CallbackInfo ci) {
        List<KeyMapping> mappings = new ArrayList<>(List.of(KeyMappings.SPELL_WHEEL_KEYMAP, KeyMappings.SPELL_WHEEL_TOGGLE_KEYMAP,
                KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP, KeyMappings.SPELLBAR_SCROLL_MODIFIER_KEYMAP));
        mappings.addAll(KeyMappings.QUICK_CAST_MAPPINGS);
        ForeignKeyMappings.unbind(mappings);
        ci.cancel();
    }
}
