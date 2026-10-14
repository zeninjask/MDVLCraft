package com.mdvlcraft.binder.music;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.MusicPlacePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Once a second, tells each player's music whether they are in a village or any other structure. Stepping into a
 * structure piece counts at once; leaving only counts once the player is more than BUFFER blocks from every piece, so
 * walking along a building's edge doesn't switch the music back and forth.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MusicPlaces {
    private static final int BUFFER = 15;
    private static final Map<UUID, MusicPlacePacket> SENT = new HashMap<>();

    private MusicPlaces() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        MusicPlacePacket last = SENT.get(player.getUUID());
        boolean village = near(level, pos, last != null && last.village() ? BUFFER : 0, true);
        boolean structure = village || near(level, pos, last != null && last.structure() ? BUFFER : 0, false);
        MusicPlacePacket place = new MusicPlacePacket(village, structure);
        if (!place.equals(SENT.put(player.getUUID(), place))) {
            BinderNetwork.sendTo(player, place);
        }
    }

    /** Whether a piece of a structure (a village only, or any) is within margin blocks of the position. */
    private static boolean near(ServerLevel level, BlockPos pos, int margin, boolean villageOnly) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkPos centre = new ChunkPos(pos);
        // a piece within the margin (under a chunk) is referenced by this chunk or a neighbour
        int chunks = margin == 0 ? 0 : 1;
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                ChunkPos chunk = new ChunkPos(centre.x + dx, centre.z + dz);
                for (StructureStart start : level.structureManager().startsForStructure(chunk,
                    s -> !villageOnly || registry.wrapAsHolder(s).is(StructureTags.VILLAGE))) {
                    for (StructurePiece piece : start.getPieces()) {
                        if (piece.getBoundingBox().inflatedBy(margin).isInside(pos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
