package com.mdvlcraft.binder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * /mdvlquest compass &lt;player&gt; &lt;structure&gt; &lt;dimension&gt; &lt;boss name&gt;: gives a lodestone compass pointing
 * at the nearest such structure in that dimension, the reward of the last quest before each structure boss (the
 * generated FTB Quests chapters, tools/generate_quests.py). The search starts from the player, moved to Nether scale
 * when the structure is in the Nether.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class QuestCompass {
    private static final int SEARCH_RADIUS_CHUNKS = 150;

    private QuestCompass() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mdvlquest")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("compass")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("structure", ResourceLocationArgument.id())
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .then(Commands.argument("name", StringArgumentType.greedyString()).executes(QuestCompass::give)))))));
    }

    private static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation structureId = ResourceLocationArgument.getId(context, "structure");
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        String name = StringArgumentType.getString(context, "name");
        Holder<Structure> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
            .getHolder(ResourceKey.create(Registries.STRUCTURE, structureId))
            .orElseThrow(() -> new IllegalArgumentException("Unknown structure " + structureId));

        BlockPos origin = searchOrigin(player, level);
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(structure), origin,
                com.mdvlcraft.binder.traits.Traits.has(player, com.mdvlcraft.binder.traits.Trait.PATHFINDER) ? 2 * SEARCH_RADIUS_CHUNKS : SEARCH_RADIUS_CHUNKS, false);   // Pathfinder: twice as far
        if (found == null) {
            player.sendSystemMessage(Component.literal("The trail to " + name + " has gone cold: no lair within reach.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack compass = new ItemStack(Items.COMPASS);
        CompoundTag tag = compass.getOrCreateTag();
        tag.put("LodestonePos", NbtUtils.writeBlockPos(found.getFirst()));
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, level.dimension())
            .result().ifPresent(dimension -> tag.put("LodestoneDimension", dimension));
        tag.putBoolean("LodestoneTracked", false);
        compass.setHoverName(Component.literal(name + " Compass").withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GOLD)));
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Points to the lair of " + name).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY)))));
        tag.getCompound("display").put("Lore", lore);
        if (!player.getInventory().add(compass)) {
            player.drop(compass, false);
        }

        return 1;
    }

    private static BlockPos searchOrigin(ServerPlayer player, ServerLevel target) {
        BlockPos pos = player.blockPosition();
        ResourceKey<Level> from = player.level().dimension();
        if (target.dimension() == from) {
            return pos;
        }

        if (target.dimension() == Level.END) {
            return BlockPos.ZERO;
        }

        if (target.dimension() == Level.NETHER) {
            return new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
        }

        return from == Level.NETHER ? new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8) : BlockPos.ZERO;
    }
}
