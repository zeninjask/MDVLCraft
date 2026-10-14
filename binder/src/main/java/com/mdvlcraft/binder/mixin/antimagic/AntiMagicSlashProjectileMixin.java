package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.entity.AntiMagicSlashProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Demon Dweller's slash no longer carves out the blocks it hits. Only the sword builds the slash with the "charged"
 * constructor; the Anti-Magic Slash spell and the Anti-Magic Demon keep breaking blocks.
 */
@Mixin(value = AntiMagicSlashProjectile.class, remap = false)
public abstract class AntiMagicSlashProjectileMixin {
    @Unique
    private boolean mdvlcraft$fromSword;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;FZ)V", at = @At("RETURN"), require = 1)
    private void mdvlcraft$markSword(Level level, LivingEntity owner, float damage, boolean charged, CallbackInfo ci) {
        this.mdvlcraft$fromSword = true;
    }

    @Redirect(method = "m_8060_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"), require = 1)
    private boolean mdvlcraft$keepBlocks(Level level, BlockPos pos, boolean drop) {
        return !this.mdvlcraft$fromSword && level.destroyBlock(pos, drop);
    }
}
