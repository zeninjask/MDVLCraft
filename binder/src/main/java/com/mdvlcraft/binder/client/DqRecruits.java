package com.mdvlcraft.binder.client;

import java.nio.file.Path;
import java.util.Map;
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

/**
 * The built-in "MDVLCraft DQ UI" resource pack (Villager Recruits' screens and the Origins pickers recoloured to the DQ9
 * palette by tools/generate_dq_skins.py, always on top), and on Recruits screens the label and fill colours its code
 * draws on those textures.
 */
public final class DqRecruits {
    private static final int VANILLA_LABEL = 0x404040;
    private static final int DQ_LABEL = 0xFFFFFF;
    /** Recruits' own fills (vanilla greys) -> the DQ greys of the recoloured textures. */
    private static final Map<Integer, Integer> FILLS = Map.of(0x0A0A0A, 0x000000, 0x3C3C3C, 0x1A1A1A, 0x505050, 0x313131, 0x646464, 0x424242, 0x8B8B8B, 0x424242);
    private static boolean recruitsScreen;

    private DqRecruits() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path root = ModList.get().getModFileById("mdvlcraft").getFile().findResource(new String[]{"resourcepacks", "dq"});
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
        recruitsScreen = isRecruits(event.getScreen());
    }

    static void onRenderPost(Post event) {
        recruitsScreen = false;
    }

    private static boolean isRecruits(Screen screen) {
        String name = screen.getClass().getName();
        return name.startsWith("com.talhanation.recruits.") || name.startsWith("com.example.villagerecruits.");
    }

    public static int fillColour(int colour) {
        if (!recruitsScreen) {
            return colour;
        }

        Integer dq = FILLS.get(colour & 0xFFFFFF);
        return dq == null ? colour : colour & 0xFF000000 | dq;
    }

    public static int textColour(int colour) {
        return recruitsScreen && (colour & 0xFFFFFF) == VANILLA_LABEL ? colour & 0xFF000000 | DQ_LABEL : colour;
    }
}
