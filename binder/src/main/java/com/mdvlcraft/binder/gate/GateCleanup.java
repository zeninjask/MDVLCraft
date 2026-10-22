package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.MDVLBinder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Gate worlds don't outlive a server run (plan section 3.5): before the worlds load, the three gate dimensions' folders are
 * deleted, so they regenerate empty (void, or fresh red-realm terrain); once the server is up, every instance left from the
 * last run is closed and every cell is free. A player who logged out inside a gate is sent home on login
 * (GateProtection). Saved progress that must survive (the Demon Castle tower's, phase 8) lives in its own saved data.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateCleanup {
    private GateCleanup() {
    }

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent event) {
        Path root = event.getServer().getWorldPath(LevelResource.ROOT).resolve("dimensions").resolve("mdvlcraft");
        for (String dimension : new String[]{"gate_realm", "red_realm", "demon_castle"}) {
            delete(root.resolve(dimension));
        }
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        int closed = GateInstances.get(event.getServer()).closeAllForRestart().size();
        if (closed > 0) {
            MDVLBinder.LOGGER.info("Gates: closed {} gate(s) left open by the last run", closed);
        }
    }

    private static void delete(Path folder) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.walk(folder)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    MDVLBinder.LOGGER.warn("Gates: couldn't delete {}: {}", path, e.toString());
                }
            });
        } catch (IOException e) {
            MDVLBinder.LOGGER.warn("Gates: couldn't clear {}: {}", folder, e.toString());
        }
    }
}
