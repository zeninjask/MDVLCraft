package com.mdvlcraft.binder.command;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.Loadout;
import com.mojang.brigadier.CommandDispatcher;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Operator commands for the ability wheel, the same actions the client sends:
 * /mdvl abilities <player>, /mdvl assign <player> <slot> <ability>, /mdvl clear <player> <slot>,
 * /mdvl cast <player> <slot>, /mdvl release <player>. Slots are 1-8.
 */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class MdvlCommand {
    private MdvlCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
                .then(Commands.literal("abilities").then(Commands.argument("player", EntityArgument.player()).executes(MdvlCommand::abilities)))
                .then(Commands.literal("assign").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, Loadout.SLOTS))
                                .then(Commands.argument("ability", ResourceLocationArgument.id()).executes(MdvlCommand::assign)))))
                .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, Loadout.SLOTS)).executes(MdvlCommand::clear))))
                .then(Commands.literal("cast").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, Loadout.SLOTS)).executes(MdvlCommand::cast))))
                .then(Commands.literal("release").then(Commands.argument("player", EntityArgument.player()).executes(MdvlCommand::release))));
    }

    private static int abilities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String granted = AbilityGrants.of(player).entrySet().stream()
                .map(entry -> entry.getKey().id() + " " + entry.getValue()).collect(Collectors.joining(", "));
        String wheel = Loadout.get(player).stream().map(slot -> slot.map(ResourceLocation::toString).orElse("-")).collect(Collectors.joining(", "));
        int mana = Math.round(MagicData.getPlayerMagicData(player).getMana());
        int maxMana = (int) player.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        context.getSource().sendSuccess(() -> Component.literal("Granted: [" + granted + "] Wheel: [" + wheel + "] Mana: " + mana + "/" + maxMana), false);
        return AbilityGrants.of(player).size();
    }

    private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation id = ResourceLocationArgument.getId(context, "ability");
        if (Ability.byId(id).isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown ability " + id));
            return 0;
        }
        if (!AbilityActions.assign(player, IntegerArgumentType.getInteger(context, "slot") - 1, Optional.of(id))) {
            context.getSource().sendFailure(Component.literal(player.getGameProfile().getName() + " has not unlocked " + id));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Assigned " + id), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.assign(EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "slot") - 1, Optional.empty());
        return 1;
    }

    private static int cast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.press(EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "slot") - 1);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AbilityActions.release(EntityArgument.getPlayer(context, "player"));
        return 1;
    }
}
