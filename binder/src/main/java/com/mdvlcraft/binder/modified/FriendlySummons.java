package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.FriendlySummonsPacket;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Tells each player which summons near them are friendly, for the marker over them (client/FriendlySummonMarker): their
 * own (Iron's summons and Modified abilities' summons, both through Iron's SummonManager) and those of players allied to
 * them (party members, by Entity.isAlliedTo, which asks Parties). Checked once a second within RANGE blocks, sent only
 * when it changes.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class FriendlySummons {
    private static final double RANGE = 48.0;
    private static final int EVERY = 20;
    /** What each player was last sent, so an unchanged list isn't sent again. */
    private static final Map<UUID, Map<Integer, Boolean>> SENT = new HashMap<>();

    private FriendlySummons() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % EVERY != 0) {
            return;
        }
        Map<Integer, Boolean> friendly = new HashMap<>();   // entity id -> whether it is the player's own
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE))) {
            Entity owner = SummonManager.getOwner(mob);
            if (!(owner instanceof Player summoner)) {
                continue;
            }
            if (summoner == player) {
                friendly.put(mob.getId(), true);
            } else if (summoner.isAlliedTo(player)) {
                friendly.put(mob.getId(), false);
            }
        }
        if (!friendly.equals(SENT.get(player.getUUID()))) {
            SENT.put(player.getUUID(), friendly);
            BinderNetwork.sendTo(player, new FriendlySummonsPacket(friendly));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
