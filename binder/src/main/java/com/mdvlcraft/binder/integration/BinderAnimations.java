package com.mdvlcraft.binder.integration;

import com.mdvlcraft.binder.superchef.ChefAnimations;
import com.mdvlcraft.binder.wukong.WukongAnimations;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;

/** Epic Fight allows one animation builder per namespace: every binder animation set (Wukong, Super Chef) is built in it. */
public final class BinderAnimations {
    private BinderAnimations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BinderAnimations::onRegistry);
    }

    private static void onRegistry(AnimationRegistryEvent event) {
        event.newBuilder("mdvlcraft", builder -> {
            WukongAnimations.build(builder);
            ChefAnimations.build(builder);
        });
    }
}
