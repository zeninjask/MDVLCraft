package com.mdvlcraft.binder.combat;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Every kind of invisibility hides a player from mobs the same way. Vanilla's Invisibility (and powers that set the
 * invisible flag, like the Shadow race's) only shrinks the range mobs see you at, and not at all with armour on, while
 * Iron's True Invisibility drops them outright; now a mob can't take an invisible player as its target at all, and one
 * already after them loses them.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class InvisibleUntargetable {
    private static final int CHECK_TICKS = 10;
    private static final double LOSE_RANGE = 48.0;

    private InvisibleUntargetable() {
    }

    public static boolean hidden(LivingEntity entity) {
        return entity instanceof Player && (entity.isInvisible() || entity.hasEffect((MobEffect)MobEffectRegistry.TRUE_INVISIBILITY.get()));
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && hidden(event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0 || !hidden(player)) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(LOSE_RANGE), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }
}
