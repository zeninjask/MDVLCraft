package com.mdvlcraft.binder.content;

import app.sedu.mc.parties.common.group.GroupType;
import app.sedu.mc.parties.common.group.ServerGroupManager;
import app.sedu.mc.parties.common.group.TrackedGroup;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.ExpSharePacket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Experience Share setting (off by default), toggled from the Social Interactions screen (client/ExpShareButton).
 * A party follows its leader's setting and only the leader can change it; a player outside a party sets their own. It
 * decides how kills are split (PartyExperience). Settings are kept in the world save, so they hold while the leader is
 * offline.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ExpShare {
    private static final String DATA = "mdvlcraft_exp_share";

    private ExpShare() {
    }

    /** Whether the player's kills are split evenly: their party leader's setting, or their own outside a party. */
    public static boolean isOn(ServerPlayer player) {
        return settings(player.server).on.contains(decider(player));
    }

    /** Toggles the setting if the player decides it, then shows every online party member the new state. */
    public static void toggle(ServerPlayer player) {
        if (!decider(player).equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Only the party leader can change Experience Share.").withStyle(ChatFormatting.RED));
            sync(player);
            return;
        }

        Settings settings = settings(player.server);
        if (!settings.on.remove(player.getUUID())) {
            settings.on.add(player.getUUID());
        }
        settings.setDirty();
        TrackedGroup party = party(player);
        if (party == null) {
            sync(player);
            return;
        }

        party.getMembers().stream()
            .map(player.server.getPlayerList()::getPlayer)
            .filter(member -> member != null)
            .forEach(ExpShare::sync);
    }

    /** Sends the player the setting they follow and whether they may change it. */
    public static void sync(ServerPlayer player) {
        BinderNetwork.sendTo(player, new ExpSharePacket(isOn(player), decider(player).equals(player.getUUID())));
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        sync((ServerPlayer)event.getEntity());
    }

    private static UUID decider(ServerPlayer player) {
        TrackedGroup party = party(player);
        return party == null ? player.getUUID() : party.getLeader();
    }

    private static TrackedGroup party(ServerPlayer player) {
        return ServerGroupManager.getGroup(player.getUUID(), GroupType.PARTY);
    }

    private static Settings settings(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Settings::load, Settings::new, DATA);
    }

    /** The players whose own setting is on. */
    private static final class Settings extends SavedData {
        private final Set<UUID> on = new HashSet<>();

        private static Settings load(CompoundTag tag) {
            Settings settings = new Settings();
            tag.getList("on", Tag.TAG_INT_ARRAY).forEach(id -> settings.on.add(NbtUtils.loadUUID(id)));
            return settings;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            on.forEach(id -> list.add(NbtUtils.createUUID(id)));
            tag.put("on", list);
            return tag;
        }
    }
}
