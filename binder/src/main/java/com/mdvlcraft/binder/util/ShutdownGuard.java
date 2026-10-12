package com.mdvlcraft.binder.util;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber(modid = "mdvlcraft")
public final class ShutdownGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long GRACE_MILLIS = 15000L;

    private ShutdownGuard() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (event.getServer().isDedicatedServer()) {
            Thread guard = new Thread(
                () -> {
                    try {
                        Thread.sleep(15000L);
                    } catch (InterruptedException e) {
                        return;
                    }

                    List<String> lingering = Thread.getAllStackTraces()
                        .keySet()
                        .stream()
                        .filter(t -> t.isAlive() && !t.isDaemon() && t != Thread.currentThread() && !"DestroyJavaVM".equals(t.getName()))
                        .map(Thread::getName)
                        .toList();
                    LOGGER.warn("Server stopped but the process is still running; threads keeping it alive: {}. Exiting.", lingering);
                    Thread fallback = new Thread(() -> {
                        try {
                            Thread.sleep(15000L);
                        } catch (InterruptedException var1x) {
                        }

                        Runtime.getRuntime().halt(0);
                    }, "MDVLCraft shutdown guard fallback");
                    fallback.setDaemon(true);
                    fallback.start();
                    System.exit(0);
                },
                "MDVLCraft shutdown guard"
            );
            guard.setDaemon(true);
            guard.start();
        }
    }
}
