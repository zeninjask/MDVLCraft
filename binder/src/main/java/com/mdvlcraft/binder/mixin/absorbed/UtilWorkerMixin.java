package com.mdvlcraft.binder.mixin.absorbed;

import com.mdvlcraft.binder.absorbed.SmoothBoot;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** From Smooth Boot (Reloaded): the background and IO executors are rebuilt with absorbed/SmoothBoot's thread counts and priorities. */
@Mixin(Util.class)
public abstract class UtilWorkerMixin {
    @Shadow
    @Final
    @Mutable
    private static ExecutorService BACKGROUND_EXECUTOR;
    @Shadow
    @Final
    @Mutable
    private static ExecutorService IO_POOL;
    @Shadow
    @Final
    private static AtomicInteger WORKER_COUNT;
    @Unique
    private static boolean mdvlcraft$mainReplaced;
    @Unique
    private static boolean mdvlcraft$ioReplaced;

    @Shadow
    private static void onThreadException(Thread thread, Throwable throwable) {
    }

    @Inject(method = "backgroundExecutor", at = @At("HEAD"))
    private static void mdvlcraft$replaceMain(CallbackInfoReturnable<Executor> cir) {
        if (!mdvlcraft$mainReplaced) {
            mdvlcraft$mainReplaced = true;
            SmoothBoot.Config config = SmoothBoot.config();
            BACKGROUND_EXECUTOR = new ForkJoinPool(config.threadCount.main, pool -> {
                SmoothBoot.Worker worker = new SmoothBoot.Worker(pool);
                worker.setPriority(config.threadPriority.main);
                worker.setName("Worker-Main-" + WORKER_COUNT.getAndIncrement());
                return worker;
            }, UtilWorkerMixin::onThreadException, true);
        }
    }

    @Inject(method = "ioPool", at = @At("HEAD"))
    private static void mdvlcraft$replaceIo(CallbackInfoReturnable<Executor> cir) {
        if (!mdvlcraft$ioReplaced) {
            mdvlcraft$ioReplaced = true;
            int priority = SmoothBoot.config().threadPriority.io;
            IO_POOL = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("IO-Worker-" + WORKER_COUNT.getAndIncrement());
                thread.setPriority(priority);
                thread.setUncaughtExceptionHandler(UtilWorkerMixin::onThreadException);
                return thread;
            });
        }
    }
}
