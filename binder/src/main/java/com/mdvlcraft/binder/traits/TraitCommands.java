package com.mdvlcraft.binder.traits;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Traits can't be obtained in play yet. For testing (ops):
 *   /mdvl trait set <player> <slot> <trait>   puts a trait in a slot (opening the slot if needed)
 *   /mdvl trait clear <player>                empties every slot
 *   /mdvl trait slots <player> <1>          how many slots the player has
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitCommands {
    private TraitCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mdvl").requires(source -> source.hasPermission(2))
            .then(Commands.literal("trait")
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, Traits.MAX_SLOTS))
                        .then(Commands.argument("trait", StringArgumentType.word())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Trait.values()).map(Trait::id), b))
                            .executes(TraitCommands::set)))))
                .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(TraitCommands::clear)))
                .then(Commands.literal("slots").then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("slots", IntegerArgumentType.integer(1, Traits.MAX_SLOTS)).executes(TraitCommands::slots))))));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Trait trait = Trait.byId(StringArgumentType.getString(context, "trait"));
        if (trait == null) {
            context.getSource().sendFailure(Component.literal("Unknown trait"));
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
        if (slot >= Traits.slots(player)) {
            Traits.setSlots(player, slot + 1);
        }
        for (int other = 0; other < Traits.MAX_SLOTS; other++) {
            if (other != slot && Traits.inSlot(player, other) == trait) {
                Traits.set(player, other, null);
            }
        }
        Traits.set(player, slot, trait);
        context.getSource().sendSuccess(() -> Component.literal("Set " + trait.id() + " in slot " + (slot + 1) + " of " + player.getScoreboardName()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        for (int slot = 0; slot < Traits.MAX_SLOTS; slot++) {
            Traits.set(player, slot, null);
        }
        context.getSource().sendSuccess(() -> Component.literal("Cleared the traits of " + player.getScoreboardName()), true);
        return 1;
    }

    private static int slots(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Traits.setSlots(player, IntegerArgumentType.getInteger(context, "slots"));
        return 1;
    }
}
