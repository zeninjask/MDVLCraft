package com.mdvlcraft.binder.client;

import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Skill callouts (ability/SkillCallout) look exactly like Cursed Fate's chant words: its own Photon effect
 * (photon:scales_of_the_dragon, glowing, jittering, shrinking away at the end, 2.1 blocks above the caster's feet), with a
 * texture of the skill's name drawn here in Minecraft's pixel font, tinted the skill's colour as dark as the chant's red,
 * and up 1.5 times as long (38 ticks, not 25). Each name's texture is made once and kept.
 */
public final class SkillCalloutClient {
    private static final ResourceLocation TEMPLATE = new ResourceLocation("photon", "fx/scales_of_the_dragon.fx");
    private static final ResourceLocation FONT = new ResourceLocation("minecraft", "textures/font/ascii.png");
    private static final int LIFETIME = 38;
    /** The chant's tint is 0x8A0000: each skill's colour is darkened the same way. */
    private static final float TINT = 0x8A / 255.0F;
    private static final int SIZE = 512;
    /** Texture pixels per font pixel: the chant's "SCALES OF THE DRAGON" fills about the texture's width at this. */
    private static final int PIXEL = 4;
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static ListTag template;
    private static NativeImage font;

    private SkillCalloutClient() {
    }

    public static void show(int entityId, Component name, int colour) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity entity = level == null ? null : level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        try {
            String text = name.getString().toUpperCase(Locale.ROOT);
            ListTag emitters = template().copy();
            CompoundTag config = emitters.getCompound(0).getCompound("config");
            config.getCompound("material").getCompound("material").putString("texture", texture(text).toString());
            config.getCompound("startColor").putInt("number", 0xFF000000 | darken(colour));
            config.getCompound("startLifetime").putFloat("number", LIFETIME);
            CompoundTag main = new CompoundTag();
            main.put("fxObjects", emitters);
            FX fx = new FX();
            fx.setFxLocation(new ResourceLocation("mdvlcraft", "callout/" + Integer.toHexString(text.hashCode())));
            fx.getMainFX().deserializeNBT(main);
            EntityEffect effect = new EntityEffect(fx, level, entity, EntityEffect.AutoRotate.NONE);
            effect.setOffset(0.0, 2.1, 0.0);
            effect.setForcedDeath(true);
            effect.setAllowMulti(true);
            effect.start();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Cursed Fate's chant effect", e);
        }
    }

    private static int darken(int colour) {
        int r = Math.round(((colour >> 16) & 0xFF) * TINT);
        int g = Math.round(((colour >> 8) & 0xFF) * TINT);
        int b = Math.round((colour & 0xFF) * TINT);
        return r << 16 | g << 8 | b;
    }

    private static ListTag template() throws IOException {
        if (template == null) {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(TEMPLATE)) {
                template = NbtIo.readCompressed(in).getList("emitters", Tag.TAG_COMPOUND);
            }
        }
        return template;
    }

    /** The name in white capitals on a clear square, centred, as Cursed Fate's chant textures are. */
    private static ResourceLocation texture(String text) throws IOException {
        ResourceLocation known = TEXTURES.get(text);
        if (known != null) {
            return known;
        }
        if (font == null) {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(FONT)) {
                font = NativeImage.read(in);
            }
        }
        int cell = font.getWidth() / 16;
        int width = 0;
        for (char c : text.toCharArray()) {
            width += advance(glyph(c), cell);
        }
        int pixel = Math.max(1, Math.min(PIXEL, SIZE / Math.max(1, width)));
        NativeImage image = new NativeImage(SIZE, SIZE, true);
        int x = (SIZE - width * pixel) / 2;
        int y = (SIZE - cell * pixel) / 2;
        for (char c : text.toCharArray()) {
            int glyph = glyph(c);
            int gx = glyph % 16 * cell;
            int gy = glyph / 16 * cell;
            for (int px = 0; px < cell; px++) {
                for (int py = 0; py < cell; py++) {
                    if ((font.getPixelRGBA(gx + px, gy + py) >>> 24) > 0 && x + (px + 1) * pixel <= SIZE && x >= 0) {
                        for (int dx = 0; dx < pixel; dx++) {
                            for (int dy = 0; dy < pixel; dy++) {
                                image.setPixelRGBA(x + px * pixel + dx, y + py * pixel + dy, 0xFFFFFFFF);
                            }
                        }
                    }
                }
            }
            x += advance(glyph, cell) * pixel;
        }
        ResourceLocation location = new ResourceLocation("mdvlcraft", "callout/" + TEXTURES.size());
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
        TEXTURES.put(text, location);
        return location;
    }

    private static int glyph(char c) {
        return c < 256 && c > ' ' ? c : c == ' ' ? ' ' : '?';
    }

    /** A glyph's width in font pixels plus one for the gap; a space is four. */
    private static int advance(int glyph, int cell) {
        if (glyph == ' ') {
            return 4;
        }
        int gx = glyph % 16 * cell;
        int gy = glyph / 16 * cell;
        for (int px = cell - 1; px >= 0; px--) {
            for (int py = 0; py < cell; py++) {
                if ((font.getPixelRGBA(gx + px, gy + py) >>> 24) > 0) {
                    return px + 2;
                }
            }
        }
        return 4;
    }
}
