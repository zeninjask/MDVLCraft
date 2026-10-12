package com.mdvlcraft.binder.content;

import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "mdvlcraft")
public final class NoMiningFatigue {
    private NoMiningFatigue() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.DIG_SLOWDOWN) {
            event.setResult(Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide() && event.player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            event.player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
    }
}
