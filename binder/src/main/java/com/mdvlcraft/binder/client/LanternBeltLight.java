package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.content.LanternBelt;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

/** A lantern on the belt lights its wearer as brightly as a lantern block, through Sodium Dynamic Lights. */
final class LanternBeltLight {
    private LanternBeltLight() {
    }

    static void register() {
        int light = Blocks.LANTERN.defaultBlockState().getLightEmission();
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER, player -> LanternBelt.wearsLantern(player) ? light : 0);
    }
}
