package com.mdvlcraft.binder.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.MDVLBinder;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Falling Leaves' atlas (fallingleaves:leaves) lists the whole textures/particle directory, so it stitched a second copy
 * of every particle texture in the pack (two 16384x8192 atlases at each resource load) to use eight leaf sprites. It now
 * loads only the sprites its leaf types (fallingleaves/leaftypes/*.json, "textures") name.
 */
@Mixin(SpriteLoader.class)
public abstract class FallingLeavesAtlasMixin {
    private static final ResourceLocation LEAVES = new ResourceLocation("fallingleaves", "leaves");

    @Shadow
    public abstract SpriteLoader.Preparations stitch(List<SpriteContents> contents, int mipLevel, Executor executor);

    @Inject(method = "loadAndStitch", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$onlyLeafSprites(ResourceManager resources, ResourceLocation atlas, int mipLevel, Executor executor,
                                           CallbackInfoReturnable<CompletableFuture<SpriteLoader.Preparations>> cir) {
        if (!LEAVES.equals(atlas)) {
            return;
        }
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
            Set<ResourceLocation> ids = new LinkedHashSet<>();
            FileToIdConverter.json("fallingleaves/leaftypes").listMatchingResources(resources).forEach((file, resource) -> {
                try (Reader reader = resource.openAsReader()) {
                    for (JsonElement texture : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("textures")) {
                        ids.add(new ResourceLocation(texture.getAsString()));
                    }
                } catch (Exception e) {
                    MDVLBinder.LOGGER.warn("Couldn't read leaf type {}", file, e);
                }
            });
            List<SpriteContents> sprites = new ArrayList<>();
            for (ResourceLocation id : ids) {
                resources.getResource(id.withPath(path -> "textures/particle/" + path + ".png")).ifPresent(resource -> {
                    SpriteContents sprite = SpriteLoader.loadSprite(id, resource);
                    if (sprite != null) {
                        sprites.add(sprite);
                    }
                });
            }
            return sprites;
        }, executor).thenApply(sprites -> stitch(sprites, mipLevel, executor)));
    }
}
