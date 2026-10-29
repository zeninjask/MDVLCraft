package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.item.custom.YamatoItem;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Yamato keeps no kill or damage count, so it shows none, nor Judgement Cut End's kill progress, and its kill reward
 * never comes (integration/YamatoUnbound).
 */
@Mixin(YamatoItem.class)
public abstract class YamatoItemMixin {
    private static final Set<String> COUNT_LINES = Set.of("tooltip.efn.yamato.kill_count", "tooltip.efn.yamato.total_damage");

    @Inject(method = "onLivingDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mdvlcraft$noDamageCount(LivingDamageEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onLivingDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mdvlcraft$noKillCount(LivingDeathEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "addSkillProgressTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void mdvlcraft$noSkillProgress(ItemStack stack, List<Component> tooltip, boolean shift, int threshold, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "m_7373_", at = @At("RETURN"), remap = false)   // appendHoverText (Nightfall is compiled to SRG names)
    private void mdvlcraft$noCountLines(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        tooltip.removeIf(line -> line.getContents() instanceof TranslatableContents text && COUNT_LINES.contains(text.getKey()));
    }
}
