package com.mdvlcraft.binder.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.p1nero.iss_ponder.client.ClientPreviewController;
import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.SpellAbility;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.createmod.ponder.enums.PonderKeybinds;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.fml.ModList;

public final class SpellPreviews {
    private static final int HOLD_TICKS = 12;
    private static final long HOVER_MILLIS = 200L;
    private static final String TREES = "data/mdvlcraft/puffish_skills/categories";
    private static final String ABILITY_REWARD = "mdvlcraft:ability";
    @Nullable
    private static Map<String, SpellPreviews.Spell> skillSpells;
    /** "category#definition" of every node definition that unlocks a spell. */
    private static final Set<String> SPELL_DEFINITIONS = new HashSet<>();
    @Nullable
    private static SpellPreviews.Spell hovered;
    private static long hoveredAt;
    private static int held;

    private SpellPreviews() {
    }

    static void hover(ResourceLocation spell, int level) {
        SpellPreviews.Spell now = new SpellPreviews.Spell(spell, level);
        if (!now.equals(hovered)) {
            held = 0;
        }

        hovered = now;
        hoveredAt = Util.getMillis();
    }

    public static void hoverSkill(ResourceLocation category, String skillId) {
        SpellPreviews.Spell spell = skillSpells().get(category + "/" + skillId);
        if (spell != null) {
            hover(spell.id(), spell.level());
        }
    }

    /** Whether a skill node with this definition unlocks a spell, so its tooltip says how to preview it. */
    public static boolean isSpellDefinition(ResourceLocation category, String definitionId) {
        skillSpells();
        return SPELL_DEFINITIONS.contains(category + "#" + definitionId);
    }

    /** Whether a spell (a skill-tree node or the abilities screen) was hovered within the last moment. */
    static boolean hovering() {
        return Util.getMillis() - hoveredAt < HOVER_MILLIS;
    }

    public static Component prompt() {
        return Component.translatable("tooltip.iss_ponder.hold_to_preview", PonderKeybinds.PONDER.message());
    }

    static void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            boolean hovering = hovered != null && Util.getMillis() - hoveredAt < 200L;
            if (hovering && PonderKeybinds.PONDER.isDown() && !ClientPreviewController.isPending() && !ClientPreviewController.isPreviewOpen()) {
                if (++held >= 12) {
                    ClientPreviewController.request(hovered.id(), hovered.level());
                    hovered = null;
                    held = 0;
                }
            } else {
                held = 0;
            }
        }
    }

    /**
     * Every skill-tree node that unlocks an Iron's spell (an mdvlcraft:ability reward), read from the trees in the binder
     * jar, which the client has too, so every tree and every spell node can be previewed. Keyed "category/node".
     */
    private static Map<String, SpellPreviews.Spell> skillSpells() {
        if (skillSpells == null) {
            Map<String, SpellPreviews.Spell> spells = new HashMap<>();
            Path categories = null;
            try {
                categories = ModList.get().getModFileById(MDVLBinder.MOD_ID).getFile().findResource(TREES);
            } catch (RuntimeException e) {
                MDVLBinder.LOGGER.error("Spell previews: the binder's mod file is missing", e);
            }
            try (Stream<Path> trees = categories == null ? Stream.empty() : Files.list(categories)) {
                for (Path tree : trees.toList()) {
                    String category = MDVLBinder.MOD_ID + ":" + tree.getFileName().toString().replace("/", "");
                    JsonObject definitions = read(tree.resolve("definitions.json"));
                    read(tree.resolve("skills.json")).entrySet().forEach(node -> {
                        JsonObject definition = definitions.getAsJsonObject(node.getValue().getAsJsonObject().get("definition").getAsString());
                        String definitionId = node.getValue().getAsJsonObject().get("definition").getAsString();
                        spell(definition).ifPresent(spell -> {
                            spells.put(category + "/" + node.getKey(), spell);
                            SPELL_DEFINITIONS.add(category + "#" + definitionId);
                        });
                    });
                }
            } catch (IOException | RuntimeException e) {
                // no previews rather than a crash on every hovered node
                MDVLBinder.LOGGER.error("Spell previews: cannot read the skill trees in {}", categories, e);
                spells.clear();
                SPELL_DEFINITIONS.clear();
            }

            skillSpells = spells;
        }

        return skillSpells;
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** The Iron's spell a node's ability reward unlocks, if it unlocks one. */
    private static Optional<SpellPreviews.Spell> spell(JsonObject definition) {
        if (definition == null || !definition.has("rewards")) {
            return Optional.empty();
        }

        for (JsonElement element : definition.getAsJsonArray("rewards")) {
            JsonObject reward = element.getAsJsonObject();
            if (ABILITY_REWARD.equals(reward.get("type").getAsString())) {
                JsonObject data = reward.getAsJsonObject("data");
                Optional<Ability> ability = Ability.byId(ResourceLocation.parse(data.get("ability").getAsString()));
                if (ability.isPresent() && ability.get() instanceof SpellAbility spellAbility) {
                    return Optional.of(new SpellPreviews.Spell(spellAbility.spell().getSpellResource(), data.get("level").getAsInt()));
                }
            }
        }
        return Optional.empty();
    }

    private record Spell(ResourceLocation id, int level) {
    }
}
