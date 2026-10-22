package com.mdvlcraft.binder.mixin.cursedfate;

import com.mdvlcraft.binder.modified.UnderworldGate;
import cursedfate.entity.IncompleteShrineEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Underworld Gate's shrine (modified/UnderworldGate) is scenery: it can't be targeted, hit, damaged or pushed. It is told
 * apart by its texture, which (unlike tags) the client also knows, so clicks and projectiles pass through it there too.
 */
@Mixin(IncompleteShrineEntity.class)
public abstract class IncompleteShrineEntityMixin extends PathfinderMob {
    protected IncompleteShrineEntityMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    private boolean mdvlcraft$gateShrine() {
        return UnderworldGate.SHRINE_TEXTURE.equals(((IncompleteShrineEntity)(Object)this).getTexture());
    }

    @Override
    public boolean isPickable() {
        return !this.mdvlcraft$gateShrine() && super.isPickable();
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.mdvlcraft$gateShrine()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$noPush(CallbackInfoReturnable<Boolean> cir) {
        if (this.mdvlcraft$gateShrine()) {
            cir.setReturnValue(false);
        }
    }
}
