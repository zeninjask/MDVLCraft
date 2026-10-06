package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
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

/**
 * Villager Recruits' screens in the Slate style of the skill screens. Its textures are recoloured by
 * tools/generate_recruits_skin.py into the binder's built-in "MDVLCraft Slate" resource pack, which is always on and
 * on top of every other pack. Recruits labels its screens in vanilla's dark grey (0x404040), which would vanish on the
 * dark panels, so while one of its screens (or Village Recruits' screens) renders that grey is drawn light instead
 * (GuiGraphicsMixin).
 */
public final class SlateRecruits {
    private static final int VANILLA_LABEL = 0x404040;
    private static final int SLATE_LABEL = 0xE8ECF2;
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

    private static boolean isRecruits(Screen screen) {
        String name = screen.getClass().getName();
        return name.startsWith("com.talhanation.recruits.") || name.startsWith("com.example.villagerecruits.");
    }

    /** A text colour as drawn now: Recruits' dark labels turn light while its screen renders. */
    public static int textColour(int colour) {
        return recruitsScreen && (colour & 0xFFFFFF) == VANILLA_LABEL ? (colour & 0xFF000000) | SLATE_LABEL : colour;
    }
}
