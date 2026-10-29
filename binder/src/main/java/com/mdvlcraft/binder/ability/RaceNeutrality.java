package com.mdvlcraft.binder.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Races some mobs leave alone. These were Apugli "mobs_ignore" powers, but Apugli isn't in the pack, so they did nothing:
 *   Illagerkin, Clan Blood: illagers;  Piglin-born, Kin of the Bastion: piglins;  Infernal, Nether Kin: piglins and
 *   zombified piglins;  Revenant, Grave Kin: undead monsters  (each unless the player hit one of them in the last 30 s);
 *   Mimic, Furniture: every mob, while the player stands still and sneaks.
 * A mob of the kind doesn't take such a player as its target, and loses them as one once a second.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RaceNeutrality {
    private static final long PROVOKED = 30 * 20;
    private static final Predicate<LivingEntity> PIGLINS = mob -> mob instanceof AbstractPiglin;
    private static final Predicate<LivingEntity> NETHER = mob -> mob instanceof AbstractPiglin || mob.getType() == EntityType.ZOMBIFIED_PIGLIN;
    private static final Predicate<LivingEntity> ILLAGERS = mob -> mob instanceof AbstractIllager;
    private static final Predicate<LivingEntity> UNDEAD = mob -> mob instanceof Enemy && mob.getMobType() == MobType.UNDEAD;
    /** Game time each player last hit a mob of a kind that is neutral to their race. */
    private static final Map<UUID, Long> HIT = new HashMap<>();

    private RaceNeutrality() {
    }

    /** Whether `mob` leaves `player` alone. */
    static boolean ignores(LivingEntity mob, Player player) {
        boolean calm = player.level().getGameTime() - HIT.getOrDefault(player.getUUID(), -PROVOKED) >= PROVOKED;
        Predicate<LivingEntity> kind = kind(player);
        if (kind != null && calm && kind.test(mob)) {
            return true;
        }
        return RaceTraits.is(player, "mimic") && player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
    }

    private static Predicate<LivingEntity> kind(Player player) {
        return RaceTraits.is(player, "illagerkin") ? ILLAGERS : RaceTraits.is(player, "piglinborn") ? PIGLINS
            : RaceTraits.is(player, "infernal") ? NETHER : RaceTraits.is(player, "revenant") ? UNDEAD : null;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && ignores(event.getEntity(), player)) {
            event.setCanceled(true);
        }
    }

    /** Hitting one of the kind provokes them all for 30 s. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            Predicate<LivingEntity> kind = kind(player);
            if (kind != null && kind.test(event.getEntity())) {
                HIT.put(player.getUUID(), player.level().getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0
            || kind(player) == null && !RaceTraits.is(player, "mimic")) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0),
            m -> m.getTarget() == player || m.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player)) {
            if (ignores(mob, player)) {
                mob.setTarget(null);
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            }
        }
    }
}
