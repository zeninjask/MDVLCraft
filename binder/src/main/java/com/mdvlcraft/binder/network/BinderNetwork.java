package com.mdvlcraft.binder.network;

import com.mdvlcraft.binder.epicskill.EquipSkillPacket;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BinderNetwork {
    private static final String PROTOCOL = "27";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("mdvlcraft", "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private BinderNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
            id++,
            SyncAbilitiesPacket.class,
            SyncAbilitiesPacket::encode,
            SyncAbilitiesPacket::decode,
            SyncAbilitiesPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            AssignSlotPacket.class,
            AssignSlotPacket::encode,
            AssignSlotPacket::decode,
            AssignSlotPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, CastPacket.class, CastPacket::encode, CastPacket::decode, CastPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            EquipSkillPacket.class,
            EquipSkillPacket::encode,
            EquipSkillPacket::decode,
            EquipSkillPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            StanceSyncPacket.class,
            StanceSyncPacket::encode,
            StanceSyncPacket::decode,
            StanceSyncPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            TechniqueCooldownsPacket.class,
            TechniqueCooldownsPacket::encode,
            TechniqueCooldownsPacket::decode,
            TechniqueCooldownsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, RespecPacket.class, RespecPacket::encode, RespecPacket::decode, RespecPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, ResetSkillsPacket.class, ResetSkillsPacket::encode, ResetSkillsPacket::decode, ResetSkillsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, SeraphFormPacket.class, SeraphFormPacket::encode, SeraphFormPacket::decode, SeraphFormPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, GauntFormPacket.class, GauntFormPacket::encode, GauntFormPacket::decode, GauntFormPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, GauntStatePacket.class, GauntStatePacket::encode, GauntStatePacket::decode, GauntStatePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, GauntClingPacket.class, GauntClingPacket::encode, GauntClingPacket::decode, GauntClingPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, GauntActionPacket.class, GauntActionPacket::encode, GauntActionPacket::decode, GauntActionPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, RaijuFormPacket.class, RaijuFormPacket::encode, RaijuFormPacket::decode, RaijuFormPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, GauntInputPacket.class, GauntInputPacket::encode, GauntInputPacket::decode, GauntInputPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, GauntAttackPacket.class, GauntAttackPacket::encode, GauntAttackPacket::decode, GauntAttackPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, JinglePacket.class, JinglePacket::encode, JinglePacket::decode, JinglePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, OpenArchetypePickPacket.class, OpenArchetypePickPacket::encode, OpenArchetypePickPacket::decode, OpenArchetypePickPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, ArchetypePickPacket.class, ArchetypePickPacket::encode, ArchetypePickPacket::decode, ArchetypePickPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            MusicPlacePacket.class,
            MusicPlacePacket::encode,
            MusicPlacePacket::decode,
            MusicPlacePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            DemonSwordArtPacket.class,
            DemonSwordArtPacket::encode,
            DemonSwordArtPacket::decode,
            DemonSwordArtPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            DismissAssimilationPacket.class,
            DismissAssimilationPacket::encode,
            DismissAssimilationPacket::decode,
            DismissAssimilationPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++,
            ArchetypeLevelsPacket.class,
            ArchetypeLevelsPacket::encode,
            ArchetypeLevelsPacket::decode,
            ArchetypeLevelsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++,
            SummonOwnerPacket.class,
            SummonOwnerPacket::encode,
            SummonOwnerPacket::decode,
            SummonOwnerPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, VehicleBlinkPacket.class, VehicleBlinkPacket::encode, VehicleBlinkPacket::decode, VehicleBlinkPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, LootViewsPacket.class, LootViewsPacket::encode, LootViewsPacket::decode, LootViewsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, ChatLinePacket.class, ChatLinePacket::encode, ChatLinePacket::decode, ChatLinePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, ExpSharePacket.class, ExpSharePacket::encode, ExpSharePacket::decode, ExpSharePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, ExpShareTogglePacket.class, ExpShareTogglePacket::encode, ExpShareTogglePacket::decode, ExpShareTogglePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, ArsenalSyncPacket.class, ArsenalSyncPacket::encode, ArsenalSyncPacket::decode, ArsenalSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, OpenArsenalPacket.class, OpenArsenalPacket::encode, OpenArsenalPacket::decode, OpenArsenalPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, BindArsenalPacket.class, BindArsenalPacket::encode, BindArsenalPacket::decode, BindArsenalPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, PickShadowPacket.class, PickShadowPacket::encode, PickShadowPacket::decode, PickShadowPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, PhantomRushPacket.class, PhantomRushPacket::encode, PhantomRushPacket::decode, PhantomRushPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, VocationPickPacket.class, VocationPickPacket::encode, VocationPickPacket::decode, VocationPickPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, PickerOpenPacket.class, PickerOpenPacket::encode, PickerOpenPacket::decode, PickerOpenPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, OpenVocationPickerPacket.class, OpenVocationPickerPacket::encode, OpenVocationPickerPacket::decode, OpenVocationPickerPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, SkillCalloutPacket.class, SkillCalloutPacket::encode, SkillCalloutPacket::decode, SkillCalloutPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, SettingsPacket.class, SettingsPacket::encode, SettingsPacket::decode, SettingsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
            id++, SettingsSyncPacket.class, SettingsSyncPacket::encode, SettingsSyncPacket::decode, SettingsSyncPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            id++, FriendlySummonsPacket.class, FriendlySummonsPacket::encode, FriendlySummonsPacket::decode, FriendlySummonsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        com.mdvlcraft.binder.gate.tower.TowerItems.registerPackets(CHANNEL, id);   // castle sigil menu, tower status line
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendToTracking(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
