package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * The pack's keybinds (resources mdvlcraft/default_keys.txt, the key lines of the pack's options.txt). Launchers keep a
 * player's own options.txt when the pack updates, so the pack's binds never reached existing installs. On the first tick
 * after the list changes they are set once (config/mdvlcraft-keys.applied remembers which list was applied); a player's
 * own rebinding after that is left alone until the pack changes its list again. The pack's Render Scale is set the same
 * way (renderScale).
 */
final class DefaultKeys {
    private static final String RESOURCE = "/mdvlcraft/default_keys.txt";
    private static boolean done;

    private DefaultKeys() {
    }

    /** The pack's Render Scale (0.7) once per install, for the same reason: a launcher keeps the old config file. */
    private static final float RENDER_SCALE = 0.7F;

    private static void renderScale() {
        Path marker = FMLPaths.CONFIGDIR.get().resolve("mdvlcraft-renderscale.applied");
        String value = Float.toString(RENDER_SCALE);
        try {
            if (Files.exists(marker) && Files.readString(marker).trim().equals(value)) {
                return;
            }
            Class<?> configClass = Class.forName("dev.zelo.renderscale.config.RenderScaleConfig");
            Object holder = Class.forName("me.shedaniel.autoconfig.AutoConfig").getMethod("getConfigHolder", Class.class).invoke(null, configClass);
            Object config = holder.getClass().getMethod("getConfig").invoke(holder);
            configClass.getField("scale").setFloat(config, RENDER_SCALE);
            holder.getClass().getMethod("save").invoke(holder);
            Files.writeString(marker, value);
            MDVLBinder.LOGGER.info("Set Render Scale to the pack's {}", RENDER_SCALE);
        } catch (ClassNotFoundException e) {
            // Render Scale isn't installed
        } catch (ReflectiveOperationException | IOException | RuntimeException e) {
            MDVLBinder.LOGGER.warn("Couldn't set the pack's Render Scale", e);
        }
    }

    static void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (done || event.phase != Phase.START || mc.options == null) {
            return;
        }
        done = true;
        renderScale();
        try (InputStream in = DefaultKeys.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Path marker = FMLPaths.CONFIGDIR.get().resolve("mdvlcraft-keys.applied");
            String hash = Integer.toHexString(text.hashCode());
            if (Files.exists(marker) && Files.readString(marker).trim().equals(hash)) {
                return;
            }
            Map<String, String> keys = new HashMap<>();
            for (String line : text.split("\n")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    keys.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                }
            }
            int set = 0;
            for (KeyMapping mapping : mc.options.keyMappings) {
                String key = keys.get(mapping.getName());
                if (key != null) {
                    mapping.setKey(InputConstants.getKey(key));
                    set++;
                }
            }
            KeyMapping.resetMapping();
            mc.options.save();
            Files.writeString(marker, hash);
            MDVLBinder.LOGGER.info("Applied the pack's {} keybinds", set);
        } catch (IOException | RuntimeException e) {
            MDVLBinder.LOGGER.warn("Couldn't apply the pack's keybinds", e);
        }
    }
}
