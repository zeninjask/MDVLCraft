package com.mdvlcraft.binder.mixin.absorbed;

import com.google.gson.JsonElement;
import com.mdvlcraft.binder.absorbed.SparseStructures;
import com.mojang.serialization.Decoder;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** From Sparse Structures: each worldgen/structure_set file is spread out (absorbed/SparseStructures) just before it is decoded. */
@Mixin(RegistryDataLoader.class)
public abstract class StructureSetSpreadMixin {
    @SuppressWarnings("rawtypes")
    @Inject(method = "loadRegistryContents", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false,
        target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private static void mdvlcraft$spread(RegistryOps.RegistryInfoLookup lookup, ResourceManager manager, ResourceKey registryKey,
            WritableRegistry registry, Decoder decoder, Map exceptions, CallbackInfo ci, String directory, FileToIdConverter converter,
            RegistryOps ops, Iterator files, Map.Entry file, ResourceLocation path, ResourceKey key, Resource resource, Reader reader,
            JsonElement json) {
        if (directory.equals("worldgen/structure_set")) {
            SparseStructures.spread(key.location().toString(), json.getAsJsonObject());
        }
    }
}
