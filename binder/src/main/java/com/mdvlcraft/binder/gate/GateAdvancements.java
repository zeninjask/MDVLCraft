package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mdvlcraft.binder.gate.tower.TowerProgress;
import com.mdvlcraft.binder.gate.tower.TowerRun;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.RegistryObject;

/**
 * Gate progress for the quest book's Gates chapter (tools/generate_quests.py). FTB Quests can't see gate events, so each
 * one grants a hidden advancement `mdvlcraft:quests/gates/<name>` (a single `minecraft:impossible` criterion, written by
 * the generator) that a quest's advancement task waits for. Most events are caught here from Forge events (opening a
 * gate, entering one, kills, veins, the forge, boss chests, Return Stones, tower floors); two need a call from their
 * owner: {@link #onCleared} (GateRewards.onCleared) and {@link #onBreak} (GateBreaks.breakGate). Also
 * `/mdvl gate quest skillxp <targets> <amount>`, the chapter's skill-experience reward (same as the clear bonus).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateAdvancements {
    /** Tower floors whose reaching has a quest. */
    private static final int[] FLOORS = {5, 10, 25, 50, 55, 100};
    /** Players this close to a breaking gate saw it break (GateBreaks' announce range). */
    private static final double BREAK_RANGE = 128.0;

    private GateAdvancements() {
    }

    /** Completes the hidden advancement `quests/gates/<name>` for the player (nothing if it is missing or done). */
    public static void grant(ServerPlayer player, String name) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "quests/gates/" + name));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    /** GateRewards.onCleared: the players present cleared the gate (red, trial and modifier gates included). */
    public static void onCleared(Instance instance, List<ServerPlayer> present) {
        RuleGates.Rule rule = RuleGates.rule(instance.id);
        boolean modified = !GateModifiers.ids(instance.id).isEmpty();
        for (ServerPlayer player : present) {
            grant(player, "clear_" + key(instance.rank));
            if (instance.red) {
                grant(player, "clear_red");
                if (instance.rank == GateRank.S) {
                    grant(player, "clear_red_s");   // its seal breaks only once the hidden boss is dead
                }
            }
            if (modified) {
                grant(player, "clear_modified");
            }
            if (rule != null) {
                grant(player, "clear_trial_" + rule.key());
            }
        }
    }

    /** GateBreaks.breakGate: everyone near the gate saw it break. */
    public static void onBreak(ServerLevel world, Instance instance) {
        for (ServerPlayer player : world.players()) {
            if (player.blockPosition().distSqr(instance.gatePos) <= BREAK_RANGE * BREAK_RANGE) {
                grant(player, "break");
            }
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getTo() == GateDimensions.DEMON_CASTLE) {
            grant(player, "tower_enter");
            return;
        }
        if (event.getTo() != GateDimensions.GATE_REALM && event.getTo() != GateDimensions.RED_REALM) {
            return;
        }
        Instance instance = GateInstances.get(player.server).at(event.getTo(), player.getX(), player.getZ());
        if (instance == null) {
            return;
        }
        grant(player, "enter_" + key(instance.rank));
        grant(player, "enter_theme_" + instance.theme.key());
        if (instance.red) {
            grant(player, "enter_red");
            if (instance.rank == GateRank.S) {
                grant(player, "enter_red_s");
            }
        }
        if (RuleGates.rule(instance.id) != null) {
            grant(player, "enter_trial");
        }
        if (!GateModifiers.ids(instance.id).isEmpty()) {
            grant(player, "enter_modified");
        }
    }

    /** A gate (not the castle's) appearing: its opener opened a gate. Rejoining after a chunk reload grants nothing new. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof GateEntity gate) || gate.getClass() != GateEntity.class) {
            return;
        }
        ServerLevel level = (ServerLevel)event.getLevel();
        Instance instance = GateInstances.get(level.getServer()).byId(gate.instanceId());
        ServerPlayer owner = instance == null || instance.owner == null ? null : level.getServer().getPlayerList().getPlayer(instance.owner);
        if (owner != null) {
            grant(owner, "open_gate");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        if (dead.getTags().contains(TowerRun.HIDDEN_TAG)) {
            for (ServerPlayer player : ((ServerLevel)dead.level()).players()) {
                if (player.distanceToSqr(dead) <= 64.0 * 64.0) {
                    grant(player, "tower_hidden");
                }
            }
        }
        if (GateSpawner.instanceOf(dead) >= 0 && event.getSource().getEntity() instanceof ServerPlayer killer) {
            grant(killer, "gate_kill");
            if ("elite".equals(dead.getPersistentData().getString(GateRewards.ROLE))) {
                grant(killer, "elite_kill");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        for (Map.Entry<GateRank, RegistryObject<Block>> ore : GateRewards.CRYSTAL_ORES.entrySet()) {
            if (state.is(ore.getValue().get())) {
                grant(player, "vein_" + key(ore.getKey()));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(GateRewards.FORGE.get())) {
            grant(player, "forge");
        } else if (state.is(GateRewards.REWARD_CHEST.get())) {
            grant(player, "boss_chest");
        }
    }

    /** A Return Stone used inside a normal gate (the only place it works). */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItemStack().is(GateRewards.RETURN_STONE.get())
            && player.level().dimension() == GateDimensions.GATE_REALM && GateInstances.get(player.server).ofPlayer(player.getUUID()) != null) {
            grant(player, "return_stone");
        }
    }

    /** Tower floors: once a second, for players in the castle. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0
            || player.level().dimension() != GateDimensions.DEMON_CASTLE) {
            return;
        }
        TowerProgress.Entry entry = TowerProgress.get(player.server).of(player.getUUID());
        for (int floor : FLOORS) {
            if (entry.best >= floor) {
                grant(player, "tower_floor_" + floor);
            }
        }
        if (entry.bestBoss >= 5) {
            grant(player, "tower_boss_5");
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate").then(Commands.literal("quest").then(Commands.literal("skillxp")
                .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(GateAdvancements::skillExperience)))))));
    }

    private static int skillExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        var players = EntityArgument.getPlayers(context, "targets");
        players.forEach(player -> GateRewards.giveExperience(player, amount));
        return players.size();
    }
}
