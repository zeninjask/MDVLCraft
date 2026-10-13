package com.mdvlcraft.binder.content;

import io.ejekta.bountiful.content.BountifulContent;
import io.ejekta.bountiful.data.Decree;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Bountiful boards are shared, so a board leans toward the players standing near it: when it rolls a new bounty and a
 * player with a matching Job or Martial is within 16 blocks, half the time the bounty comes from one of that player's
 * decrees instead of the board's own (see mixin/bountiful/BoardBlockEntityMixin).
 */
public final class BountyLean {
    private static final double RANGE = 16.0;
    private static final float CHANCE = 0.5F;
    private static final String HUNTER = "mdvlcraft_hunter";
    private static final List<ResourceLocation> LAYERS = List.of(
        ResourceLocation.fromNamespaceAndPath("origins-classes", "class"), ResourceLocation.fromNamespaceAndPath("mdvlcraft", "martial"));
    /** Origin id → the Bountiful decrees that suit it. Origins not listed (Nitwit) don't lean the board. */
    private static final Map<String, List<String>> DECREES = Map.ofEntries(
        Map.entry("origins-classes:blacksmith", List.of("armorer", "toolsmith")),
        Map.entry("origins-classes:cleric", List.of("cleric")),
        Map.entry("origins-classes:cook", List.of("butcher", "farmer")),
        Map.entry("origins-classes:farmer", List.of("farmer")),
        Map.entry("origins-classes:merchant", List.of("librarian", "mapper")),
        Map.entry("origins-classes:rancher", List.of("butcher", "shepherd", "leatherer")),
        Map.entry("origins-classes:miner", List.of("toolsmith", "armorer", "inventor")),
        Map.entry("origins-classes:lumberjack", List.of("fletcher")),
        Map.entry("origins-classes:explorer", List.of("mapper")),
        Map.entry("mdvlcraft:alchemist", List.of("cleric")),
        Map.entry("mdvlcraft:sailor", List.of("fisherman")),
        Map.entry("mdvlcraft:fisher", List.of("fisherman")),
        Map.entry("mdvlcraft:bard", List.of("librarian")),
        Map.entry("mdvlcraft:herbalist", List.of("farmer")),
        Map.entry("mdvlcraft:beekeeper", List.of("farmer")),
        Map.entry("mdvlcraft:gravedigger", List.of("cleric", HUNTER)),
        Map.entry("origins-classes:warrior", List.of(HUNTER, "armorer")),
        Map.entry("origins-classes:archer", List.of(HUNTER, "fletcher")),
        Map.entry("origins-classes:rogue", List.of(HUNTER, "leatherer")),
        Map.entry("origins-classes:beastmaster", List.of(HUNTER, "butcher")),
        Map.entry("mdvlcraft:cavalry", List.of(HUNTER, "leatherer")),
        Map.entry("mdvlcraft:duelist", List.of(HUNTER)),
        Map.entry("mdvlcraft:guardian", List.of(HUNTER, "armorer")),
        Map.entry("mdvlcraft:skirmisher", List.of(HUNTER)),
        Map.entry("mdvlcraft:crossbowman", List.of(HUNTER, "fletcher")),
        Map.entry("mdvlcraft:spearman", List.of(HUNTER)),
        Map.entry("mdvlcraft:monster_hunter", List.of(HUNTER)),
        Map.entry("mdvlcraft:bounty_hunter", List.of(HUNTER)),
        Map.entry("mdvlcraft:battle_mage", List.of(HUNTER, "librarian")),
        Map.entry("mdvlcraft:sentinel", List.of(HUNTER, "fletcher")),
        Map.entry("mdvlcraft:paladin", List.of(HUNTER, "cleric")),
        Map.entry("mdvlcraft:corsair", List.of(HUNTER, "fisherman")),
        Map.entry("mdvlcraft:zealot", List.of(HUNTER, "cleric"))
    );

    private BountyLean() {
    }

    /** The decrees for the board's next bounty: usually its own, sometimes a nearby player's. */
    public static Set<Decree> decreesFor(BlockEntity board, Set<Decree> own) {
        RandomSource random = board.getLevel().getRandom();
        if (random.nextFloat() >= CHANCE) {
            return own;
        }

        List<String> choices = board.getLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(board.getBlockPos()).inflate(RANGE)).stream()
            .flatMap(player -> LAYERS.stream().map(layer -> originId(player, layer)))
            .filter(DECREES::containsKey)
            .flatMap(origin -> DECREES.get(origin).stream())
            .toList();
        if (choices.isEmpty()) {
            return own;
        }

        String id = choices.get(random.nextInt(choices.size()));
        Set<Decree> decrees = BountifulContent.INSTANCE.getDecrees(Set.of(id));
        if (decrees.isEmpty()) {
            throw new IllegalStateException("Bountiful has no decree " + id);
        }

        return decrees;
    }

    private static String originId(ServerPlayer player, ResourceLocation layerId) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), layerId);
        ResourceKey<Origin> origin = IOriginContainer.get(player).map(container -> container.getOrigin(layer)).orElse(null);
        return origin == null ? "" : origin.location().toString();
    }
}
