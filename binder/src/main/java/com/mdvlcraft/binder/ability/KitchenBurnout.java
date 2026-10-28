package com.mdvlcraft.binder.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Malevolent Kitchen leaves no Cursed Fate BurnOut behind: the effect Cursed Fate gives when a domain or shrine ends (it
 * locks techniques for a while) never lands on a player who has Malevolent Kitchen (the Master Chef).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class KitchenBurnout {
    private static final ResourceLocation BURNOUT = ResourceLocation.fromNamespaceAndPath("cursedfate", "burnout");

    private KitchenBurnout() {
    }

    @SubscribeEvent
    public static void onApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && BURNOUT.equals(ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect()))
            && AbilityGrants.level(player, Technique.MALEVOLENT_KITCHEN) > 0) {
            event.setResult(Event.Result.DENY);
        }
    }
}
