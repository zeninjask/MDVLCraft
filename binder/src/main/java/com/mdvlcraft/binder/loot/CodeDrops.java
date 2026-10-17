package com.mdvlcraft.binder.loot;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Drops mods make in code rather than in a loot table, which loot/LootViews adds to the mob's JER page. Each comes from
 * the named class; the counts and chances are copied from it.
 */
final class CodeDrops {
    private static final Map<String, List<CodeDrops.Drop>> DROPS = Map.ofEntries(
        // WitherBoss.dropCustomDeathLoot
        Map.entry("minecraft:wither", List.of(new CodeDrops.Drop("minecraft:nether_star", 1, 1, 1.0F, 0))),
        // Arachne's and Leonidas's ForgeEvents.onLivingDrops
        Map.entry("epicfight_arachne:arachne", List.of(new CodeDrops.Drop("epicfight_arachne:arachne_egg", 1, 1, 1.0F, 0))),
        Map.entry("epicfight_leonidas:leonidas", List.of(new CodeDrops.Drop("epicfight_leonidas:the_bottomless_pouch", 1, 1, 1.0F, 0))),
        // NecromancerEntity: the tarnished crown it wears drops 15% of the time
        Map.entry("irons_spellbooks:necromancer", List.of(new CodeDrops.Drop("irons_spellbooks:tarnished_helmet", 1, 1, 0.15F, DropView.PLAYER_KILL))),
        // Mimic's ForgeEvents.onLivingDrops
        Map.entry("mimic:proteus", List.of(new CodeDrops.Drop("mimic:proteus_commendation", 1, 1, 1.0F, 0))),
        // Cursed Fate's MobDropsProcedure, when a player (or their pet) kills
        Map.entry("cursedfate:shinjuku_showdown_gojoa", List.of(
            new CodeDrops.Drop("cursedfate:shinjuku_sorcerer_cloth", 2, 2, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:cursed_amethyst_bottle", 1, 1, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:special_grade_residue", 1, 3, 1.0F, DropView.PLAYER_KILL)
        )),
        Map.entry("cursedfate:shinjuku_showdown_sukunaa", List.of(
            new CodeDrops.Drop("cursedfate:fallen_sorcerer_cloth", 2, 2, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:cursed_amethyst_bottle", 1, 1, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:special_grade_residue", 1, 3, 1.0F, DropView.PLAYER_KILL)
        )),
        Map.entry("cursedfate:yuji_itadori", List.of(
            new CodeDrops.Drop("cursedfate:blessed_sorcerer_cloth", 2, 2, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:death_painting", 1, 1, 1.0F / 3.0F, DropView.PLAYER_KILL)
        )),
        Map.entry("cursedfate:mouth_curse", CodeDrops.curse()),
        Map.entry("cursedfate:elephant_curse", CodeDrops.curse()),
        Map.entry("cursedfate:rugby_curse", CodeDrops.curse())
    );

    private CodeDrops() {
    }

    private static List<CodeDrops.Drop> curse() {
        return List.of(
            new CodeDrops.Drop("cursedfate:curse_residue", 1, 3, 1.0F, DropView.PLAYER_KILL),
            new CodeDrops.Drop("cursedfate:jujutsu_token", 1, 1, 1.0F / 16.0F, DropView.PLAYER_KILL)
        );
    }

    static List<DropView> of(ResourceLocation entity) {
        return DROPS.getOrDefault(entity.toString(), List.of()).stream().map(CodeDrops.Drop::view).toList();
    }

    /** Throws when an entry names an entity type or item that isn't registered. */
    static void check() {
        DROPS.forEach((entity, drops) -> {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(entity))) {
                throw new IllegalStateException("CodeDrops names an unregistered entity type: " + entity);
            }

            drops.forEach(CodeDrops.Drop::view);
        });
    }

    private record Drop(String item, int min, int max, float chance, int flags) {
        DropView view() {
            Item registered = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(this.item));
            if (registered == null || !ResourceLocation.parse(this.item).equals(ForgeRegistries.ITEMS.getKey(registered))) {
                throw new IllegalStateException("CodeDrops names an unregistered item: " + this.item);
            }

            return new DropView(new ItemStack(registered), this.min, this.max, this.chance, this.flags);
        }
    }
}
