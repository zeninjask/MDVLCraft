package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;

/**
 * The BD&Hill Reforge the Sword Edge resource pack swaps Epic Fight's Uchigatana for a 3D model with no display
 * transforms, so the blade is drawn out of the hand. The binder's "bdhill_fix" pack gives that model its transforms. It
 * only works on top of BD&Hill (the model's mesh and texture are BD&Hill's), so it is offered, always on and on top,
 * only while BD&Hill is selected; the repository asks again on every resource reload, including after the pack screen.
 */
final class BdHillFix {
    /** Part of the BD&Hill pack's file name, as it appears in its pack ID ("file/【Released】BDHill Reforge ..."). */
    private static final String BDHILL = "BDHill";

    private BdHillFix() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        Path root = ModList.get().getModFileById(MDVLBinder.MOD_ID).getFile().findResource("resourcepacks", "bdhill_fix");
        event.addRepositorySource(consumer -> {
            if (Minecraft.getInstance().options.resourcePacks.stream().noneMatch(id -> id.contains(BDHILL))) {
                return;
            }
            Pack pack = Pack.readMetaAndCreate(MDVLBinder.MOD_ID + ":bdhill_fix", Component.literal("MDVLCraft BD&Hill fixes"), true,
                    id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack == null) {
                throw new IllegalStateException("MDVLCraft BD&Hill fix resource pack is missing its pack.mcmeta");
            }
            consumer.accept(pack);
        });
    }
}
