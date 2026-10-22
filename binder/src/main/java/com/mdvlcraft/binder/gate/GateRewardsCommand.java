package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Debug commands to test rewards without the builder and spawner, under /mdvl gate reward (run as a player inside a gate):
 *   tag <targets> <normal|elite|boss>  makes the targets gate mobs of the caller's gate with that role
 *   vein                               a crystal vein of the gate's rank at the command's position
 *   clear                              marks the caller's gate CLEARED and runs GateRewards.onCleared
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateRewardsCommand {
    private GateRewardsCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("gate").then(Commands.literal("reward")
                .then(Commands.literal("tag").then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("role", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("normal", "elite", "boss"), b))
                        .executes(GateRewardsCommand::tag))))
                .then(Commands.literal("vein").executes(GateRewardsCommand::vein))
                .then(Commands.literal("clear").executes(GateRewardsCommand::clear)))));
    }

    private static Instance instance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Instance instance = GateInstances.get(player.server).ofPlayer(player.getUUID());
        if (instance == null || instance.dimension != player.level().dimension()) {
            context.getSource().sendFailure(Component.literal("Not inside a gate"));
            return null;
        }
        return instance;
    }

    private static int tag(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Instance instance = instance(context);
        if (instance == null) {
            return 0;
        }
        String role = StringArgumentType.getString(context, "role");
        int n = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof LivingEntity) {
                entity.getPersistentData().putInt(GateSpawner.TAG, instance.id);
                if (role.equals("normal")) {
                    entity.getPersistentData().remove(GateRewards.ROLE);
                } else {
                    entity.getPersistentData().putString(GateRewards.ROLE, role);
                }
                n++;
            }
        }
        int tagged = n;
        context.getSource().sendSuccess(() -> Component.literal("Tagged " + tagged + " as " + role + " of gate " + instance.id), true);
        return n;
    }

    private static int vein(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Instance instance = instance(context);
        if (instance == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateRewards.vein((ServerLevel)player.level(), BlockPos.containing(context.getSource().getPosition()),
            GateRewards.CRYSTAL_ORES.get(instance.rank).get().defaultBlockState(), 4, player.getRandom());
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Instance instance = instance(context);
        if (instance == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        instance.state = GateInstances.State.CLEARED;
        GateInstances.get(player.server).setDirty();
        GateRewards.onCleared((ServerLevel)player.level(), instance);
        context.getSource().sendSuccess(() -> Component.literal("Cleared gate " + instance.id), true);
        return 1;
    }
}
