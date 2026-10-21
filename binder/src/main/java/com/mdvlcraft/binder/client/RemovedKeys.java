package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.mixin.KeyMappingAccessor;
import com.mdvlcraft.binder.mixin.OptionsAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

final class RemovedKeys {
    private static final Set<String> NAMES = Set.of(
        "key.advancements",
        "key.puffish_skills.open",
        "key.origins.view_origin",
        "key.elixirum.collection",
        "key.recruits.team_screen_key",
        "key.recruits.map_screen_key",
        "key.entityculling.toggle",
        "key.epicfight.config",
        "key.epicfight.switch_vanilla_model_debug",
        "key.modernfix.config",
        "key.curios.open.desc",
        "key.ftbquests.quests",
        "key.ftbteams.open_gui",
        "key.accurateblockplacement.toggle",
        "key.esoteric_spell.toggle_hidden_ring",
        "soundsbegone.config",   // B by default, the backpack key
        "key.saveToolbarActivator",
        "key.loadToolbarActivator"
    );
    /** Every key of these categories goes, except the KEEP ones. */
    private static final Set<String> CATEGORIES = Set.of(
        "key.categories.puzzleslib",
        "key.categories.voicechat",
        "fdlib.key_category",
        "key.categories.placebo",
        "key.betterlockon.category",
        "key.category.invmove.main",
        "key.categories.combat_evolution",
        "key.categories.puzzlesapi",
        "key.categories.traveloptics",
        "key.categories.ftbquests",
        "key.categories.ftbteams"
    );
    /** Every key whose name starts with one of these goes, except the KEEP ones. */
    private static final Set<String> PREFIXES = Set.of("key.shouldersurfing.", "gui.xaero_", "key.betterlockon.", "key.jade.", "keybind.invmove.", "key.jei.", "key.simplytooltips.", "key.sword_soaring.", "key.efn.",
        "key.p1nero_ec.");
    private static final Set<String> KEEP = Set.of(
        "key.push_to_talk", "key.shouldersurfing.swap_shoulder", "gui.xaero_open_map",
        "key.jei.showRecipe", "key.jei.closeRecipeGui", "key.jei.bookmark", "key.jei.toggleOverlay"
    );
    /** The JEI keys that stay share this one category ("JEI"), in place of JEI's several. */
    private static final String JEI = "key.categories.mdvlcraft.jei";
    private static KeyMapping[] pruned;

    private RemovedKeys() {
    }

    static void onClientTick(ClientTickEvent event) {
        Options options = Minecraft.getInstance().options;
        if (event.phase == Phase.START && options != null && options.keyMappings != pruned) {
            prune(options);
        }
    }

    private static void prune(Options options) {
        KeyMapping[] kept = Arrays.stream(options.keyMappings).filter(key -> !removed(key)).toArray(KeyMapping[]::new);
        if (kept.length != options.keyMappings.length) {
            Arrays.stream(options.keyMappings).filter(RemovedKeys::removed).forEach(key -> key.setKey(InputConstants.UNKNOWN));
            ((OptionsAccessor)options).mdvlcraft$setKeyMappings(kept);
            KeyMappingAccessor.mdvlcraft$all().values().removeIf(RemovedKeys::removed);
            KeyMapping.resetMapping();
            MDVLBinder.LOGGER.debug("Removed {} keys now opened from the MDVLCraft menu", options.keyMappings.length - kept.length);
        }

        mergeJei(options.keyMappings);
        pruned = options.keyMappings;
    }

    private static void mergeJei(KeyMapping[] keys) {
        Map<String, Integer> order = KeyMappingAccessor.mdvlcraft$categoryOrder();
        for (KeyMapping key : keys) {
            if (key.getName().startsWith("key.jei.") && !JEI.equals(key.getCategory())) {
                order.putIfAbsent(JEI, order.get(key.getCategory()));
                ((KeyMappingAccessor)key).mdvlcraft$setCategory(JEI);
            }
        }
    }

    static boolean removed(KeyMapping key) {
        String name = key.getName();
        return NAMES.contains(name)
            || !KEEP.contains(name) && (CATEGORIES.contains(key.getCategory()) || PREFIXES.stream().anyMatch(name::startsWith));
    }
}
