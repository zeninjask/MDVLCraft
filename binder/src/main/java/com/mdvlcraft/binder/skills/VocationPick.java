package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.OpenVocationPickerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The vocation picker (client/picker/VocationPicker): a player who has picked no archetype is sent it on login, and
 * picking a vocation unlocks both its archetype nodes on the Archetypes page, which opens the vocation's tab (ArchetypeTabs).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class VocationPick {
    private VocationPick() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        if (Respec.picked(player).isEmpty()) {
            BinderNetwork.sendTo(player, new OpenVocationPickerPacket());
        }
    }

    public static void pick(ServerPlayer player, String first, String second) {
        if (!Respec.picked(player).isEmpty() || VocationParts.tabOf(first, second).isEmpty() || first.equals(second)) {
            return;
        }
        Respec.node(first).unlock(player);
        Respec.node(second).unlock(player);
    }
}
