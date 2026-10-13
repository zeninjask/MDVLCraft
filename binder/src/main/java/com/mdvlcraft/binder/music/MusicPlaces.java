package com.mdvlcraft.binder.music;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.MusicPlacePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.StructureManager;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Once a second, tells each player's music whether they stand inside a village or any other structure piece. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MusicPlaces {
    private static final Map<UUID, MusicPlacePacket> SENT = new HashMap<>();

    private MusicPlaces() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        StructureManager structures = player.serverLevel().structureManager();
        BlockPos pos = player.blockPosition();
        boolean village = structures.getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid();
        boolean structure = village
            || structures.getAllStructuresAt(pos).keySet().stream().anyMatch(s -> structures.getStructureWithPieceAt(pos, s).isValid());
        MusicPlacePacket place = new MusicPlacePacket(village, structure);
        if (!place.equals(SENT.put(player.getUUID(), place))) {
            BinderNetwork.sendTo(player, place);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
