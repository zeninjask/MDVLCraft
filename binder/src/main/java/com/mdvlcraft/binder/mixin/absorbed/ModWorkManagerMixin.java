package com.mdvlcraft.binder.mixin.absorbed;

import com.mdvlcraft.binder.absorbed.SmoothBoot;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import net.minecraftforge.fml.ModWorkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** From Smooth Boot (Reloaded): mod loading workers run at absorbed/SmoothBoot's mod loading priority. */
@Mixin(value = ModWorkManager.class, remap = false)
public abstract class ModWorkManagerMixin {
    /**
     * @author abdelaziz (Smooth Boot)
     * @reason a configurable priority for mod loading threads
     */
    @Overwrite
    private static ForkJoinWorkerThread newForkJoinWorkerThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName("modloading-worker-" + thread.getPoolIndex());
        thread.setPriority(SmoothBoot.config().threadPriority.modLoading);
        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        return thread;
    }
}
