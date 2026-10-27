package com.mdvlcraft.binder.absorbed;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * From Smooth Boot (Reloaded) (abdelaziz, MIT): fewer, lower-priority background worker threads, so the game thread
 * keeps the CPU while the game starts. Read from config/smoothboot.json (the pack's file) before Minecraft starts; the
 * worker swaps are mixin/absorbed/UtilWorkerMixin, ModWorkManagerMixin and the client/server Main mixins.
 */
public final class SmoothBoot {
    public static final Logger LOGGER = LogManager.getLogger("Smooth Boot");
    private static Config config;

    private SmoothBoot() {
    }

    /** config/smoothboot.json; a missing or unreadable file uses the defaults. */
    public static Config config() {
        if (config == null) {
            Path path = Path.of(System.getProperty("user.dir"), "config", "smoothboot.json");
            try (Reader reader = Files.newBufferedReader(path)) {
                config = new Gson().fromJson(reader, Config.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("No readable {}, using defaults", path);
            }
            if (config == null) {
                config = new Config();
            }
            config.validate();
        }
        return config;
    }

    public static class Config {
        public ThreadCount threadCount = new ThreadCount();
        public ThreadPriority threadPriority = new ThreadPriority();

        void validate() {
            threadCount.bootstrap = Math.max(threadCount.bootstrap, 1);
            threadCount.main = Math.max(threadCount.main, 1);
            threadPriority.game = Mth.clamp(threadPriority.game, 1, 10);
            threadPriority.integratedServer = Mth.clamp(threadPriority.integratedServer, 1, 10);
            threadPriority.bootstrap = Mth.clamp(threadPriority.bootstrap, 1, 10);
            threadPriority.main = Mth.clamp(threadPriority.main, 1, 10);
            threadPriority.io = Mth.clamp(threadPriority.io, 1, 10);
            threadPriority.modLoading = Mth.clamp(threadPriority.modLoading, 1, 10);
        }
    }

    public static class ThreadCount {
        public int bootstrap = 1;
        public int main = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    public static class ThreadPriority {
        public int game = 5;
        public int bootstrap = 1;
        public int main = 1;
        public int io = 1;
        public int integratedServer = 5;
        public int modLoading = 1;
    }

    /** A worker that logs when it dies. */
    public static final class Worker extends ForkJoinWorkerThread {
        public Worker(ForkJoinPool pool) {
            super(pool);
        }

        @Override
        protected void onTermination(Throwable throwable) {
            if (throwable != null) {
                LOGGER.warn("{} died", this.getName(), throwable);
            }
            super.onTermination(throwable);
        }
    }
}
