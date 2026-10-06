package com.mdvlcraft.binder.mixin.swordsoaring;

import com.mdvlcraft.binder.client.ForeignKeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.p1nero.ss.client.keymapping.SwordSoaringKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

/**
 * Sword Soaring's flight keys (take off, boost, flight mode) and its sword skill key (for its Sword Dominion skills,
 * which nothing teaches in the pack) are unbound and never registered. The Vatansever's sword-return key stays.
 */
@Mixin(value = SwordSoaringKeyMappings.class, remap = false)
public abstract class SwordSoaringKeyMappingsMixin {
    @Unique
    private static final Set<String> mdvlcraft$REMOVED = Set.of("key.sword_soaring.take_off", "key.sword_soaring.acceleration",
            "key.sword_soaring.switch_mode", "key.sword_soaring.sword_skill");

    @Redirect(method = "registerKeys", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V"))
    private static void mdvlcraft$skipFlightKeys(RegisterKeyMappingsEvent event, KeyMapping mapping) {
        if (mdvlcraft$REMOVED.contains(mapping.getName())) {
            ForeignKeyMappings.unbind(List.of(mapping));
        } else {
            event.register(mapping);
        }
    }
}
