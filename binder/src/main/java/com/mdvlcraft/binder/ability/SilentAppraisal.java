package com.mdvlcraft.binder.ability;

import com.cursedfate_roslon_meimei.CursedfateRoslonMeimeiMod;
import com.cursedfate_roslon_meimei.network.MeiMeiCrowCameraViewPacket;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Upkeep for Silent Appraisal (Cursed Fate Meimei's crow camera): it costs {@link #MANA_PER_SECOND} mana a second, and
 * the camera ends cleanly - crow gone, view back on the player, the eye in front of the player's face removed - when
 * the mana runs out or the crow flies past what the player's client can still see (the crow's tracking reaches the
 * server's view distance, see mixin/EntityTypeMixin). Meimei itself only removes the eye when its owner sneaks.
 */
public final class SilentAppraisal {
    public static final float MANA_PER_SECOND = 3.0F;
    public static final String CAMERA_TYPE = "cursedfate_roslon_meimei:mei_mei_crow_camera";
    private static final String ACTIVE_CAMERA = "MeiMeiCrowEntityActiveCameraUUID";
    private static final String LIFE_TICKS = "MeiMeiCrowEntityLifeTicks";
    private static final String EYE_FX = "photon:meimei_ability_1_2";
    private static final String CROW_FX = "photon:meimei_ability_1_1";

    private SilentAppraisal() {
    }

    static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String active = player.getPersistentData().getString(ACTIVE_CAMERA);
            if (!active.isEmpty()) {
                Entity crow = player.serverLevel().getEntity(UUID.fromString(active));
                if (crow == null || crow.isRemoved()) {
                    end(player, null);
                } else if (outOfSight(player, crow) || crow.getPersistentData().getInt(LIFE_TICKS) % 20 == 0 && !pay(player)) {
                    end(player, crow);
                }
            }
        }
    }

    /** Past the distance the server tracks entities and sends chunks to this player. */
    private static boolean outOfSight(ServerPlayer player, Entity crow) {
        double reach = (player.server.getPlayerList().getViewDistance() - 1) * 16.0;
        double dx = crow.getX() - player.getX();
        double dz = crow.getZ() - player.getZ();
        return dx * dx + dz * dz > reach * reach;
    }

    private static boolean pay(ServerPlayer player) {
        if (player.isCreative()) {
            return true;
        }

        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < MANA_PER_SECOND) {
            return false;
        }

        magic.setMana(magic.getMana() - MANA_PER_SECOND);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    private static void end(ServerPlayer player, Entity crow) {
        player.getPersistentData().remove(ACTIVE_CAMERA);
        CursedfateRoslonMeimeiMod.PACKET_HANDLER.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new MeiMeiCrowCameraViewPacket(false, -1));
        command(player, "photon fx remove entity @s true " + EYE_FX);
        if (crow != null) {
            command(crow, "photon fx remove entity @s true " + CROW_FX);
            crow.discard();
        }
    }

    private static void command(Entity entity, String command) {
        ServerLevel level = (ServerLevel)entity.level();
        level.getServer().getCommands().performPrefixedCommand(entity.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
    }
}
