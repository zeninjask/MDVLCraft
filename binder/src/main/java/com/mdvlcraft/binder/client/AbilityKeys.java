package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.CastPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

/**
 * Abilities screen, ability wheel (hold) and cast keys. Every letter key is already taken in the pack, so the defaults
 * are [, ` and Caps Lock; {@link #logConflicts} warns when a binding shares a key with another mod.
 */
public final class AbilityKeys {
    private static final String CATEGORY = "key.categories.mdvlcraft";
    public static final KeyMapping OPEN_ABILITIES = key("open_abilities", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping WHEEL = key("ability_wheel", GLFW.GLFW_KEY_GRAVE_ACCENT);
    public static final KeyMapping CAST = key("cast_ability", GLFW.GLFW_KEY_CAPS_LOCK);
    private static final List<KeyMapping> ALL = List.of(OPEN_ABILITIES, WHEEL, CAST);

    private static boolean castDown;

    private AbilityKeys() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.mdvlcraft." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ALL.forEach(event::register);
    }

    /** Logs other key mappings bound to the same keys as the binder's, so pack defaults can be kept conflict-free. */
    static void logConflicts(Minecraft minecraft) {
        for (KeyMapping mine : ALL) {
            Arrays.stream(minecraft.options.keyMappings)
                    .filter(other -> other != mine && other.same(mine))
                    .forEach(other -> MDVLBinder.LOGGER.warn("Key {} ({}) is also bound to {}", mine.getName(), mine.getKey().getName(), other.getName()));
        }
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.player == null) {
            return;
        }
        while (OPEN_ABILITIES.consumeClick()) {
            minecraft.setScreen(new AbilityScreen());
        }
        while (WHEEL.consumeClick()) {
            minecraft.setScreen(new WheelScreen());
        }
        boolean tapped = false;
        while (CAST.consumeClick()) {
            tapped = true;
        }
        boolean down = CAST.isDown() && minecraft.screen == null;
        if (tapped && !down && !castDown) {
            // Pressed and released between two ticks: still a cast (and its release).
            BinderNetwork.sendToServer(new CastPacket(true, ClientAbilities.selected()));
            BinderNetwork.sendToServer(new CastPacket(false, ClientAbilities.selected()));
        } else if (down != castDown) {
            castDown = down;
            BinderNetwork.sendToServer(new CastPacket(down, ClientAbilities.selected()));
        }
    }
}
