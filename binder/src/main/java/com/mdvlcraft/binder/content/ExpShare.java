package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.ExpSharePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Each player's Experience Share setting (off by default), toggled from the Social Interactions screen
 * (client/ExpShareButton) and kept through death. It decides how that player's kills are split (PartyExperience).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ExpShare {
    private static final String KEY = "mdvlcraft:exp_share";

    private ExpShare() {
    }

    public static boolean isOn(ServerPlayer player) {
        return persisted(player).getBoolean(KEY);
    }

    public static void toggle(ServerPlayer player) {
        CompoundTag persisted = persisted(player);
        persisted.putBoolean(KEY, !persisted.getBoolean(KEY));
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        BinderNetwork.sendTo(player, new ExpSharePacket(isOn(player)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        BinderNetwork.sendTo(player, new ExpSharePacket(isOn(player)));
    }

    private static CompoundTag persisted(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }
}
