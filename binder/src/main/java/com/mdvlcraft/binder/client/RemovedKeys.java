package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.mixin.KeyMappingAccessor;
import com.mdvlcraft.binder.mixin.OptionsAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

final class RemovedKeys {
    static final Set<String> NAMES = Set.of(
        "key.advancements",
        "key.puffish_skills.open",
        "key.origins.view_origin",
        "key.elixirum.collection",
        "key.recruits.team_screen_key",
        "key.recruits.map_screen_key",
        "key.voice_chat",
        "gui.xaero_open_settings",
        "gui.xaero_minimap_settings"
    );
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
        KeyMapping[] kept = Arrays.stream(options.keyMappings).filter(key -> !NAMES.contains(key.getName())).toArray(KeyMapping[]::new);
        if (kept.length != options.keyMappings.length) {
            Arrays.stream(options.keyMappings).filter(key -> NAMES.contains(key.getName())).forEach(key -> key.setKey(InputConstants.UNKNOWN));
            ((OptionsAccessor)options).mdvlcraft$setKeyMappings(kept);
            KeyMappingAccessor.mdvlcraft$all().keySet().removeIf(NAMES::contains);
            KeyMapping.resetMapping();
            MDVLBinder.LOGGER.debug("Removed {} keys now opened from the MDVLCraft menu", options.keyMappings.length - kept.length);
        }

        pruned = options.keyMappings;
    }
}
