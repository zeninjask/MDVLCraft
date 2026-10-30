package com.mdvlcraft.binder.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The pack's effect mods ship about 2,150 particle textures, many 512 to 2300 px wide (Cursed Fate's domains, Nightfall's
 * thunder, 512-wide animation strips), and every one is stitched: a 16384x8192 particle atlas, half a gigabyte of video
 * memory, a long stitch at every resource load and too big for some graphics cards. A sprite wider than MAX (in practice
 * only particles: blocks, items and GUI sprites are far smaller) is scaled down to MAX wide as it loads, an animation
 * strip keeping its frame count. The atlas fits 8192x8192; at the size particles are drawn the difference barely shows.
 */
@Mixin(SpriteLoader.class)
public abstract class ParticleSpriteSizeMixin {
    private static final int MAX = 384;

    @Redirect(method = "loadSprite", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;read(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;"))
    private static NativeImage mdvlcraft$downscale(InputStream in, ResourceLocation id, Resource resource) throws IOException {
        NativeImage image = NativeImage.read(in);
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= MAX) {
            return image;
        }
        int newHeight = height > width && height % width == 0 ? MAX * (height / width)   // a strip of square frames
            : Math.max(1, Math.round(height * (float)MAX / width));
        NativeImage scaled = new NativeImage(image.format(), MAX, newHeight, false);
        image.resizeSubRectTo(0, 0, width, height, scaled);
        image.close();
        return scaled;
    }
}
