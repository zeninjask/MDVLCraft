package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.modified.Summons;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * How a player's summons pick targets. Casting a summon spell again while its summons are out cycles the mode (sneaking
 * instead casts the recast, which dismisses them; ability/AbilityActions):
 * passive (attack nothing), guardian (only what attacks the summoner), attack (what the summoner attacks) and hostile
 * (those, and every hostile mob nearby; the default). Applies to every summon in Iron's SummonManager: Iron's, Magic From
 * the East's and the Modified abilities'. A filter refuses targets the mode does not allow, and twice a second each
 * summon without a target is given one the mode wants.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SummonModes {
    private static final String KEY = "mdvlcraft_summon_mode";
    private static final int INTERVAL = 10;
    /** How long after the summoner hits something it still counts as the summoner's target (ticks). */
    private static final int ATTACK_MEMORY = 100;
    private static final double HOSTILE_RANGE = 16.0;
    private static int ticks;

    public enum Mode {
        PASSIVE, GUARDIAN, ATTACK, HOSTILE;

        Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private SummonModes() {
    }

    public static Mode mode(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(KEY) ? Mode.values()[data.getInt(KEY)] : Mode.HOSTILE;
    }

    /** Whether casting `spell` now would be its recast while its summons are out. */
    public static boolean summonsOut(MagicData magic, AbstractSpell spell) {
        RecastInstance recast = magic.getPlayerRecasts().getRecastInstance(spell.getSpellId());
        return recast != null && recast.getCastData() instanceof SummonedEntitiesCastData;
    }

    public static void cycle(ServerPlayer player) {
        Mode mode = mode(player).next();
        player.getPersistentData().putInt(KEY, mode.ordinal());
        player.displayClientMessage(Component.translatable("message.mdvlcraft.summon_mode." + mode.name().toLowerCase())
            .withStyle(ChatFormatting.AQUA), true);
        for (UUID id : SummonManager.getSummons(player)) {
            if (player.serverLevel().getEntity(id) instanceof Mob summon && summon.getTarget() != null && !allows(player, mode, summon.getTarget())) {
                summon.setTarget(null);
            }
        }
    }

    private static boolean allows(ServerPlayer owner, Mode mode, LivingEntity target) {
        return switch (mode) {
            case PASSIVE -> false;
            case GUARDIAN -> attacksOwner(owner, target);
            case ATTACK -> attackedByOwner(owner, target);
            case HOSTILE -> attacksOwner(owner, target) || attackedByOwner(owner, target) || target instanceof Enemy;
        };
    }

    private static boolean attacksOwner(ServerPlayer owner, LivingEntity target) {
        return target == owner.getLastHurtByMob() || target instanceof Mob mob && mob.getTarget() == owner;
    }

    private static boolean attackedByOwner(ServerPlayer owner, LivingEntity target) {
        return target == owner.getLastHurtMob() && owner.tickCount - owner.getLastHurtMobTimestamp() < ATTACK_MEMORY;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && !event.getEntity().level().isClientSide() && SummonManager.getOwner(event.getEntity()) instanceof ServerPlayer owner
            && !allows(owner, mode(owner), target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END || ++ticks % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer owner : event.getServer().getPlayerList().getPlayers()) {
            Mode mode = mode(owner);
            if (mode == Mode.PASSIVE) {
                continue;
            }
            for (UUID id : SummonManager.getSummons(owner)) {
                if (owner.serverLevel().getEntity(id) instanceof Mob summon && summon.isAlive() && summon.getTarget() == null) {
                    LivingEntity target = pick(owner, mode, summon);
                    if (target != null) {
                        summon.setTarget(target);
                    }
                }
            }
        }
    }

    private static LivingEntity pick(ServerPlayer owner, Mode mode, Mob summon) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && !Summons.allied(summon, attacker)) {
            return attacker;
        }
        LivingEntity hit = owner.getLastHurtMob();
        if (mode != Mode.GUARDIAN && hit != null && hit.isAlive() && attackedByOwner(owner, hit) && !Summons.allied(summon, hit)) {
            return hit;
        }
        if (mode != Mode.HOSTILE) {
            return null;
        }
        return summon.level().getEntitiesOfClass(Mob.class, summon.getBoundingBox().inflate(HOSTILE_RANGE),
                mob -> mob instanceof Enemy && mob.isAlive() && !Summons.allied(summon, mob) && summon.hasLineOfSight(mob))
            .stream().min(Comparator.comparingDouble(summon::distanceToSqr)).orElse(null);
    }

}
