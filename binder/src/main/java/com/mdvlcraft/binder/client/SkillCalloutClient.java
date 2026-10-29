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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Skill callouts (ability/SkillCallout) look exactly like Cursed Fate's chant words: its own Photon effect
 * (photon:scales_of_the_dragon, glowing, jittering, shrinking away at the end) at a random spot to the left or right of the
 * caster's head (beside), with a
 * texture of the skill's name drawn here in Minecraft's pixel font, large and in the skill's full colour so a short name
 * reads as well as a long chant, and up 1.5 times as long (38 ticks, not 25). Each name's texture is made once and kept.
 */
public final class SkillCalloutClient {
    private static final ResourceLocation TEMPLATE = new ResourceLocation("photon", "fx/scales_of_the_dragon.fx");
    private static final ResourceLocation FONT = new ResourceLocation("minecraft", "textures/font/ascii.png");
    private static final int LIFETIME = 38;
    private static final int SIZE = 512;
    /** The most texture pixels per font pixel: a short name is drawn large, up to about the texture's width. */
    private static final int PIXEL = 12;
    /** Share of the texture's width a name may fill. */
    private static final float FILL = 0.9F;
    /** How far out to the side of the head a name is centred, in blocks: past the face, never far. */
    private static final double SIDE_MIN = 0.8;
    private static final double SIDE_MAX = 1.1;
    /** How far behind the face a name may sit; never in front. */
    private static final double BEHIND_MAX = 0.3;
    /** The head's height, down from the top of the entity. */
    private static final double HEAD = 0.45;
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static ListTag template;
    private static NativeImage font;

    private SkillCalloutClient() {
    }

    /**
     * The name to call out. A skill some mod left without a translation would read as its raw key
     * (skill.epicfight.battojutsu): the key's last word stands in for it instead, underscores as spaces.
     */
    private static String label(Component name) {
        String text = name.getString();
        boolean untranslated = name.getContents() instanceof TranslatableContents key && !I18n.exists(key.getKey());
        if (untranslated || !text.contains(" ") && text.matches("[a-z0-9_]+(\\.[a-z0-9_]+)+")) {
            String word = text.substring(text.lastIndexOf('.') + 1).replace('_', ' ');
            return word.isEmpty() ? text : word;
        }
        return text;
    }

    public static void show(int entityId, Component name, int colour) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity entity = level == null ? null : level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        try {
            String text = label(name).toUpperCase(Locale.ROOT);
            ListTag emitters = template().copy();
            CompoundTag config = emitters.getCompound(0).getCompound("config");
            config.getCompound("material").getCompound("material").putString("texture", texture(text).toString());
            config.getCompound("startColor").putInt("number", 0xFF000000 | colour);
            config.getCompound("startLifetime").putFloat("number", LIFETIME);
            CompoundTag main = new CompoundTag();
            main.put("fxObjects", emitters);
            FX fx = new FX();
            fx.setFxLocation(new ResourceLocation("mdvlcraft", "callout/" + Integer.toHexString(text.hashCode())));
            fx.getMainFX().deserializeNBT(main);
            EntityEffect effect = new EntityEffect(fx, level, entity, EntityEffect.AutoRotate.NONE);
            Vec3 offset = beside(entity);
            effect.setOffset(offset.x, offset.y, offset.z);
            effect.setForcedDeath(true);
            effect.setAllowMulti(true);
            effect.start();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Cursed Fate's chant effect", e);
        }
    }

    /**
     * A random spot beside the head, from the entity's feet: left or right, out to the side far enough that the name
     * clears the face, level with it or a little behind (never in front), between the bottom and the top of the head.
     */
    private static Vec3 beside(Entity entity) {
        RandomSource random = entity.level().getRandom();
        float yaw = entity.getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x).scale(random.nextBoolean() ? 1.0 : -1.0);
        double out = Mth.nextDouble(random, SIDE_MIN, SIDE_MAX);
        double along = Mth.nextDouble(random, -BEHIND_MAX, 0.0);
        double top = entity.getBbHeight();
        double height = Mth.nextDouble(random, top - HEAD, top);
        return side.scale(out).add(forward.scale(along)).add(0.0, height, 0.0);
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
        int pixel = Math.max(1, Math.min(PIXEL, (int)(SIZE * FILL) / Math.max(1, width)));
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
