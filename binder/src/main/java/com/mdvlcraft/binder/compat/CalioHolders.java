package com.mdvlcraft.binder.compat;

import io.github.edwinmindcraft.calio.common.access.MappedRegistryAccess;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class CalioHolders {
    private CalioHolders() {
    }

    public static <T> Holder<T> getOrCreate(MappedRegistryAccess<T> access, ResourceKey<T> key) {
        synchronized (lock(access)) {
            Holder<T> holder = access.calio$getOrCreateHolderOrThrow(key);
            return holder != null ? holder : access.calio$getOrCreateHolderOrThrow(key);
        }
    }

    private static Object lock(Object registry) {
        if (FMLEnvironment.dist.isClient() && registry instanceof MappedRegistry<?> mapped) {
            Object lock = CalioDynamicRegistryManager.getClientInstance().getLock((ResourceKey)mapped.key());
            if (lock != null) {
                return lock;
            }
        }

        return registry;
    }
}
