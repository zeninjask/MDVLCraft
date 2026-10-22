package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Red-gate landscape hazards (plan 7.2), from the theme file's "hazard" (GateThemes.Hazard), applied every 5 seconds to
 * each roster player inside a red gate that is still sealed (ACTIVE):
 *   cold    Slowness I, and 1 freezing damage unless the player wears something warm (the vanilla freeze_immune_wearables
 *           tag: leather armour) or stands within FIRE_RANGE of a lit campfire
 *   hunger  that much food exhaustion (4 = one hunger point): heat, and the snowfield's scarce food
 *   spores  Poison I for 3 s while standing on or in one of the listed blocks
 * Normal (room) gates have no hazards; creative and spectator players are left alone.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateHazards {
    private static final int PERIOD = 100;
    private static final int FIRE_RANGE = 6;
    private static int tick;

    private GateHazards() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tick % PERIOD != 0) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(GateDimensions.RED_REALM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        GateInstances gates = GateInstances.get(event.getServer());
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            Instance instance = gates.ofPlayer(player.getUUID());
            if (instance == null || !instance.red || instance.state != GateInstances.State.ACTIVE || instance.dimension != GateDimensions.RED_REALM) {
                continue;
            }
            GateThemes.Theme data = GateThemes.get(instance.theme);
            if (data != null && data.hazard().any()) {
                apply(level, player, data.hazard());
            }
        }
    }

    static void apply(ServerLevel level, ServerPlayer player, GateThemes.Hazard hazard) {
        if (hazard.cold()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PERIOD + 20, 0, true, false, true));
            if (!warm(level, player)) {
                player.hurt(level.damageSources().freeze(), 1.0f);
            }
        }
        if (hazard.hunger() > 0f) {
            player.causeFoodExhaustion(hazard.hunger());
        }
        if (!hazard.spores().isEmpty()) {
            BlockPos feet = player.blockPosition();
            if (hazard.spores().contains(level.getBlockState(feet).getBlock()) || hazard.spores().contains(level.getBlockState(feet.below()).getBlock())) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        }
    }

    /** Warm clothing, or a lit campfire close by. */
    private static boolean warm(ServerLevel level, ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                return true;
            }
        }
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-FIRE_RANGE, -2, -FIRE_RANGE), at.offset(FIRE_RANGE, 2, FIRE_RANGE))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT)) {
                return true;
            }
        }
        return false;
    }
}
