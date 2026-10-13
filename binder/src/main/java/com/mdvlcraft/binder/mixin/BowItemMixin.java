package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.combat.EnchantmentScaling;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Power adds 80% of its extra arrow damage. */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setBaseDamage(D)V"))
    private void mdvlcraft$scalePower(AbstractArrow arrow, double damage) {
        double bonus = damage - arrow.getBaseDamage();
        arrow.setBaseDamage(arrow.getBaseDamage() + bonus * EnchantmentScaling.SCALE);
    }
}
