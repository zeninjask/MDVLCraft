package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.TraitMagic;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood Price: before Iron's checks a cast, a player short of mana pays the rest in health (TraitMagic.bloodPrice). */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class BloodPriceMixin {
    @Inject(method = "canBeCastedBy", at = @At("HEAD"))
    private void mdvlcraft$bloodPrice(int level, CastSource source, MagicData magic, Player player, CallbackInfoReturnable<CastResult> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            TraitMagic.bloodPrice((AbstractSpell)(Object)this, level, source, magic, serverPlayer);
        }
    }
}
