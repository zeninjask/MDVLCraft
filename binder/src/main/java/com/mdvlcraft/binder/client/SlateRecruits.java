package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
import java.util.Map;

/**
 * Villager Recruits' screens in the Slate style of the skill screens. Its textures are recoloured by
 * tools/generate_recruits_skin.py into the binder's built-in "MDVLCraft Slate" resource pack, which is always on and
 * on top of every other pack. (The pack also carries display transforms for the BD&Hill resource pack's 3D Uchigatana,
 * whose model has none, so the blade was drawn out of the hand.) While one of its screens (or Village Recruits' screens) renders, vanilla's dark grey
 * labels (0x404040), which would vanish on the dark panels, are drawn light and the plain greys of its list rows and
 * drop-downs become Slate shades (GuiGraphicsMixin), its buttons are drawn as Slate boxes (AbstractButtonMixin) and its
 * tooltips get the Slate colours.
 */
public final class SlateRecruits {
    private static final int VANILLA_LABEL = 0x404040;
    private static final int SLATE_LABEL = 0xE8ECF2;
    /** Recruits' row and drop-down greys -> Slate shades (RGB; alpha is kept). */
    private static final Map<Integer, Integer> FILLS = Map.of(
            0x0A0A0A, 0x0E1014,  // selected
            0x3C3C3C, 0x1C2027,  // row
            0x505050, 0x22262E,  // row (groups, factions)
            0x646464, 0x2C3139,  // hovered
            0x8B8B8B, 0x1C2027); // drop-down box (hire screen)
    private static boolean recruitsScreen;

    private SlateRecruits() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        Path root = ModList.get().getModFileById(MDVLBinder.MOD_ID).getFile().findResource("resourcepacks", "slate");
        Pack pack = Pack.readMetaAndCreate(MDVLBinder.MOD_ID + ":slate", Component.literal("MDVLCraft Slate UI"), true,
                id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack == null) {
            throw new IllegalStateException("MDVLCraft Slate resource pack is missing its pack.mcmeta");
        }
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    static void onRenderPre(ScreenEvent.Render.Pre event) {
        recruitsScreen = isRecruits(event.getScreen());
    }

    static void onRenderPost(ScreenEvent.Render.Post event) {
        recruitsScreen = false;
    }

    static boolean isRecruits(Screen screen) {
        String name = screen.getClass().getName();
        return name.startsWith("com.talhanation.recruits.") || name.startsWith("com.example.villagerecruits.");
    }

    /** True while a Recruits screen renders: its buttons are drawn by {@link #drawButton}. */
    public static boolean styled() {
        return recruitsScreen;
    }

    /** A Slate button box; the caller draws the label on top in {@link #buttonText}. */
    public static void drawButton(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean active) {
        SlateGui.button(graphics, x, y, width, height, !active ? SlateGui.Button.INACTIVE : hovered ? SlateGui.Button.HOVERED : SlateGui.Button.NORMAL);
    }

    public static int buttonText(boolean active) {
        return active ? SlateGui.TEXT : SlateGui.MUTED;
    }

    /** A fill colour as drawn now: Recruits' greys turn Slate while its screen renders. */
    public static int fillColour(int colour) {
        if (!recruitsScreen) {
            return colour;
        }
        Integer slate = FILLS.get(colour & 0xFFFFFF);
        return slate == null ? colour : (colour & 0xFF000000) | slate;
    }

    /** A text colour as drawn now: Recruits' dark labels turn light while its screen renders. */
    public static int textColour(int colour) {
        return recruitsScreen && (colour & 0xFFFFFF) == VANILLA_LABEL ? (colour & 0xFF000000) | SLATE_LABEL : colour;
    }
}
