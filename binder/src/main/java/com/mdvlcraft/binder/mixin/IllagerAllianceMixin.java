package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.modified.Summons;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Pillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pillagers, evokers and illusioners count every illager as an ally; a summoned illager (Terracotta Guard) is on its owner's side instead. */
@Mixin({Pillager.class, Evoker.class, Illusioner.class})
public abstract class IllagerAllianceMixin {
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$summonSide(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (!Summons.kind(self).isEmpty() || !Summons.kind(other).isEmpty()) {
            cir.setReturnValue(Summons.allied(self, other));
        }
    }
}
