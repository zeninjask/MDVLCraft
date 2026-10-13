package com.mdvlcraft.binder.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraftforge.client.event.ScreenEvent.Render.Post;
import net.minecraftforge.client.event.ScreenEvent.Render.Pre;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

/**
 * The built-in "MDVLCraft DQ UI" resource pack: container screens, Villager Recruits, JEI, the Origins pickers, toasts
 * and every other vanilla-style GUI texture recoloured to the DQ9 palette by tools/generate_dq_skins.py, always on top.
 * Those textures are black now, so on their screens the dark grey label colour their code draws is drawn white, along
 * with Recruits' own grey fills; on toasts the recipe and tutorial toasts' black and dark purple text turn white and
 * pale blue (mixin/GuiGraphicsMixin, ToastComponentMixin).
 */
public final class DqSkins {
    private static final int VANILLA_LABEL = 0x404040;
    private static final int TOAST_TEXT = 0x000000;
    private static final int TOAST_TITLE = 0x500050;
    /** Recruits' own fills (vanilla greys) -> the DQ greys of the recoloured textures. */
    private static final Map<Integer, Integer> FILLS = Map.of(0x0A0A0A, 0x000000, 0x3C3C3C, 0x1A1A1A, 0x505050, 0x313131, 0x646464, 0x424242, 0x8B8B8B, 0x424242);
    /** Package -> whether its mod's screens draw on recoloured textures; filled as screens are first seen. */
    private static final Map<String, Boolean> SKINNED_PACKAGES = new HashMap<>();
    private static Path root;
    private static Set<String> skinnedMods;
    private static boolean skinnedScreen;
    private static boolean toasts;

    private DqSkins() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            root = ModList.get().getModFileById("mdvlcraft").getFile().findResource(new String[]{"resourcepacks", "dq"});
            Pack pack = Pack.readMetaAndCreate(
                "mdvlcraft:dq",
                Component.literal("MDVLCraft DQ UI"),
                true,
                id -> new PathPackResources(id, root, true),
                PackType.CLIENT_RESOURCES,
                Position.TOP,
                PackSource.BUILT_IN
            );
            if (pack == null) {
                throw new IllegalStateException("MDVLCraft DQ UI resource pack is missing its pack.mcmeta");
            }

            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }

    static void onRenderPre(Pre event) {
        skinnedScreen = isSkinned(event.getScreen());
    }

    static void onRenderPost(Post event) {
        skinnedScreen = false;
    }

    /** Set while the toasts draw (mixin/ToastComponentMixin). */
    public static void drawingToasts(boolean drawing) {
        toasts = drawing;
    }

    /**
     * A screen draws on recoloured textures when it is Minecraft's own, its mod has textures in the DQ pack, or its mod
     * has no GUI textures at all (so it draws on vanilla's or another mod's, like the Recruits addon).
     */
    private static boolean isSkinned(Screen screen) {
        String pkg = screen.getClass().getPackageName();
        return pkg.startsWith("net.minecraft.") || SKINNED_PACKAGES.computeIfAbsent(pkg, DqSkins::skinnedPackage);
    }

    private static boolean skinnedPackage(String pkg) {
        IModFileInfo owner = ModList.get().getModFiles().stream()
            .filter(file -> file.getFile().getSecureJar().getPackages().contains(pkg))
            .findFirst()
            .orElse(null);
        if (owner == null) {
            return false;
        }

        boolean dq = owner.getMods().stream().anyMatch(mod -> skinnedMods().contains(mod.getModId()));
        return dq || !hasGuiTextures(owner.getFile().getSecureJar().getRootPath());
    }

    private static Set<String> skinnedMods() {
        if (skinnedMods == null) {
            try (Stream<Path> namespaces = Files.list(root.resolve("assets"))) {
                // Directory names in a jar keep their trailing slash.
                skinnedMods = namespaces.map(path -> path.getFileName().toString().replace("/", "")).collect(Collectors.toUnmodifiableSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return skinnedMods;
    }

    private static boolean hasGuiTextures(Path jarRoot) {
        Path assets = jarRoot.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return false;
        }

        try (Stream<Path> namespaces = Files.list(assets)) {
            return namespaces.map(namespace -> namespace.resolve("textures").resolve("gui")).anyMatch(Files::isDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int fillColour(int colour) {
        if (!skinnedScreen) {
            return colour;
        }

        Integer dq = FILLS.get(colour & 0xFFFFFF);
        return dq == null ? colour : colour & 0xFF000000 | dq;
    }

    public static int textColour(int colour) {
        int rgb = colour & 0xFFFFFF;
        if (skinnedScreen && rgb == VANILLA_LABEL || toasts && rgb == TOAST_TEXT) {
            return colour & 0xFF000000 | DqGui.TEXT & 0xFFFFFF;
        }

        if (toasts && rgb == TOAST_TITLE) {
            return colour & 0xFF000000 | DqGui.HEADING & 0xFFFFFF;
        }

        return colour;
    }
}
