package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.item.AbstractAntiMagicSwordItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Carrying a demon sword without Anti-Magic's Five-Leaf Grimoire gave the carrier Weakness III and Silence every tick: the
 * Weakness took 12 attack damage (every weapon read 0 Attack Damage) and the Silence blocked every ability, including
 * the summon spell that dismisses the sword. Only that self-penalty is dropped; the swords still silence what they hit.
 */
@Mixin(value = AbstractAntiMagicSwordItem.class, remap = false)
public abstract class AntiMagicSwordPenaltyMixin {
    @Redirect(method = "m_6883_", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 2)
    private boolean mdvlcraft$noGrimoirePenalty(Player player, MobEffectInstance effect) {
        return false;
    }
}
