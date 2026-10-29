package com.mdvlcraft.binder.command;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.AbilityActions;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.Loadout;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "mdvlcraft")
public final class MdvlCommand {
    private MdvlCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                            "mdvl"
                                        )
                                        .requires(source -> source.hasPermission(2)))
                                    .then(
                                        Commands.literal("abilities")
                                            .then(Commands.argument("player", EntityArgument.player()).executes(MdvlCommand::abilities))
                                    ))
                                .then(
                                    Commands.literal("assign")
                                        .then(
                                            Commands.argument("player", EntityArgument.player())
                                                .then(
                                                    Commands.argument("slot", IntegerArgumentType.integer(1, 8))
                                                        .then(Commands.argument("ability", ResourceLocationArgument.id()).executes(MdvlCommand::assign))
                                                )
                                        )
                                ))
                            .then(
                                Commands.literal("clear")
                                    .then(
                                        Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("slot", IntegerArgumentType.integer(1, 8)).executes(MdvlCommand::clear))
                                    )
                            ))
                        .then(
                            Commands.literal("cast")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 8)).executes(MdvlCommand::cast))
                                )
                        ))
                    .then(Commands.literal("release").then(Commands.argument("player", EntityArgument.player()).executes(MdvlCommand::release))))
                .then(Commands.literal("weapons").executes(MdvlCommand::weapons))
                .then(Commands.literal("rigs").executes(MdvlCommand::rigs))
                .then(Commands.literal("obtainable").executes(MdvlCommand::obtainable))
                .then(Commands.literal("dump").executes(MdvlCommand::dump))
                .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("ability", ResourceLocationArgument.id())
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, 10)).executes(MdvlCommand::grant)))))
                .then(Commands.literal("respec").then(Commands.argument("players", EntityArgument.players()).executes(MdvlCommand::respec)))
        );
    }

    /** Opens the archetype picker for the players; the respec they confirm keeps their whole level (Respec.offerFree). */
    private static int respec(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int offered = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
            if (com.mdvlcraft.binder.skills.Respec.offerFree(player)) {
                offered++;
            } else {
                ((CommandSourceStack)context.getSource()).sendFailure(Component.literal(player.getScoreboardName() + " has no vocation (two archetypes) to change"));
            }
        }

        int count = offered;
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Free respec offered to " + count + " player(s)"), true);
        return count;
    }

    private static int rigs(CommandContext<CommandSourceStack> context) {
        List<String> lines = RigReport.build(context.getSource().getServer().overworld());
        Path file = context.getSource().getServer().getServerDirectory().toPath().resolve("rigs.tsv");
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        long none = lines.stream().filter(line -> line.split("\t")[1].equals("-")).count();
        context.getSource().sendSuccess(() -> Component.literal((lines.size() - 1) + " living entities, " + none + " without a patch; written to " + file), false);
        return (int)none;
    }

    /** Writes entities.tsv (every entity type's id, size, category and class) and spells.tsv (every Iron's spell id and school). */
    private static int dump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        net.minecraft.server.level.ServerLevel level = source.getLevel();
        List<String> entities = new ArrayList<>(List.of("id\twidth\theight\tcategory\tclass"));
        for (net.minecraft.world.entity.EntityType<?> type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES) {
            String cls = "?";
            try {
                net.minecraft.world.entity.Entity entity = type.create(level);
                if (entity != null) {
                    cls = entity.getClass().getName() + (entity instanceof net.minecraft.world.entity.Mob ? " mob" : "");
                    entity.discard();
                }
            } catch (RuntimeException | LinkageError e) {
                cls = "error";
            }
            entities.add(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type) + "\t" + type.getWidth() + "\t" + type.getHeight()
                + "\t" + type.getCategory() + "\t" + cls);
        }
        List<String> spells = new ArrayList<>(List.of("id\tschool"));
        io.redspace.ironsspellbooks.api.registry.SpellRegistry.REGISTRY.get().getValues()
            .forEach(spell -> spells.add(spell.getSpellId() + "\t" + spell.getSchoolType().getId()));
        List<String> enchantments = new ArrayList<>(List.of("id\tcategory\tcurse"));
        for (net.minecraft.world.item.enchantment.Enchantment enchantment : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS) {
            enchantments.add(net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment) + "\t"
                + (enchantment.category == null ? "none" : enchantment.category.name()) + "\t" + enchantment.isCurse());
        }
        try {
            Path dir = source.getServer().getServerDirectory().toPath();
            Files.write(dir.resolve("entities.tsv"), entities);
            Files.write(dir.resolve("spells.tsv"), spells);
            Files.write(dir.resolve("enchantments.tsv"), enchantments);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        source.sendSuccess(() -> Component.literal(entities.size() - 1 + " entity types, " + (spells.size() - 1) + " spells written"), false);
        return entities.size();
    }

    private static int weapons(CommandContext<CommandSourceStack> context) {
        List<WeaponReport.Row> rows = WeaponReport.build();
        Path file = ((CommandSourceStack)context.getSource()).getServer().getServerDirectory().toPath().resolve("weapons.tsv");
        List<String> lines = new ArrayList<>(List.of("item\tsupported\tcategory\tweapon_type\tclass"));
        rows.forEach(
            row -> lines.add(String.join("\t", row.id().toString(), String.valueOf(row.supported()), row.category(), row.weaponType(), row.itemClass()))
        );

        try {
            Files.write(file, lines);
        } catch (IOException var6) {
            throw new UncheckedIOException(var6);
        }

        long unsupported = rows.stream().filter(row -> !row.supported()).count();
        ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal(rows.size() + " weapons, " + unsupported + " without Epic Fight support; written to " + file), false);
        return (int)unsupported;
    }

    private static int obtainable(CommandContext<CommandSourceStack> context) {
        ObtainabilityReport.Result result = ObtainabilityReport.build(((CommandSourceStack)context.getSource()).getServer());
        Path file = ((CommandSourceStack)context.getSource()).getServer().getServerDirectory().toPath().resolve("obtainability.tsv");
        List<String> lines = new ArrayList<>(List.of("item\tsource\thidden"));
        result.rows().forEach(row -> lines.add(row.id() + "\t" + (row.source() == null ? "-" : row.source()) + "\t" + row.hidden()));
        result.problems().forEach(problem -> lines.add("# " + problem));

        try {
            Files.write(file, lines);
        } catch (IOException var5) {
            throw new UncheckedIOException(var5);
        }

        int missing = result.missing().size();
        ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal(result.rows().size() + " items, " + missing + " with no survival source that JEI still lists; written to " + file), false);
        return missing;
    }

    private static int abilities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String granted = AbilityGrants.of(player)
            .entrySet()
            .stream()
            .map(entry -> entry.getKey().id() + " " + entry.getValue())
            .collect(Collectors.joining(", "));
        String wheel = Loadout.get(player).stream().map(slot -> slot.map(ResourceLocation::toString).orElse("-")).collect(Collectors.joining(", "));
        int mana = Math.round(MagicData.getPlayerMagicData(player).getMana());
        int maxMana = (int)player.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get());
        ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal("Granted: [" + granted + "] Wheel: [" + wheel + "] Mana: " + mana + "/" + maxMana), false);
        return AbilityGrants.of(player).size();
    }

    /** Gives an ability at a level (0 takes it back), outside the skill trees: for testing. */
    private static int grant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation id = ResourceLocationArgument.getId(context, "ability");
        int level = IntegerArgumentType.getInteger(context, "level");
        return Ability.byId(id).map(ability -> {
            AbilityGrants.give(player, ability, level);
            context.getSource().sendSuccess(() -> Component.literal((level > 0 ? "Granted " : "Took back ") + id), false);
            return 1;
        }).orElseGet(() -> {
            context.getSource().sendFailure(Component.literal("Unknown ability " + id));
            return 0;
        });
    }

    private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation id = ResourceLocationArgument.getId(context, "ability");
        if (Ability.byId(id).isEmpty()) {
            ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("Unknown ability " + id));
            return 0;
        } else if (!AbilityActions.assign(player, IntegerArgumentType.getInteger(context, "slot") - 1, Optional.of(id))) {
            ((CommandSourceStack)context.getSource()).sendFailure(Component.literal(player.getGameProfile().getName() + " has not unlocked " + id));
            return 0;
        } else {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Assigned " + id), false);
            return 1;
        }
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
