package com.mdvlcraft.binder.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class HerobrineLook {
    private static final ResourceLocation HEROBRINE = new ResourceLocation("true_herobrine", "herobrine");
    private static final double RANGE = 256.0;

    private HerobrineLook() {
    }

    public static void register() {
        if (ModList.get().isLoaded(HEROBRINE.getNamespace())) {
            MinecraftForge.EVENT_BUS.addListener(HerobrineLook::onLivingTick);
        }
    }

    private static void onLivingTick(LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && HEROBRINE.equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))) {
            Player player = mob.level().getNearestPlayer(mob, 256.0);
            if (player != null && !player.isSpectator()) {
                mob.getLookControl().setLookAt(player, 30.0F, mob.getMaxHeadXRot());
            }
        }
    }
}
