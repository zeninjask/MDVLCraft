package com.mdvlcraft.binder.mixin.irons;

import com.mdvlcraft.binder.ability.ManaRegen;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every drop in a player's mana (spells, techniques, upkeep drains; addMana goes through setMana) pauses regeneration (ManaRegen). */
@Mixin(value = MagicData.class, remap = false)
public abstract class MagicDataMixin {
    @Shadow
    private ServerPlayer serverPlayer;
    @Shadow
    private float mana;

    @Inject(method = "setMana", at = @At("HEAD"))
    private void mdvlcraft$noteSpend(float newMana, CallbackInfo ci) {
        if (this.serverPlayer != null && newMana < this.mana) {
            ManaRegen.spent(this.serverPlayer);
        }
    }
}
