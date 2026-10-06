package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.StanceSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Samurai's Stance. Casting it switches the current element on or off; sneaking while casting changes the element
 * (an active stance changes with it). The element is saved with the player; being active is not, so the stance is
 * off after logging in or respawning. Every nearby client is told the player's stance to colour their weapon trail.
 */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class Stances {
    private static final String ELEMENT_KEY = "mdvlcraft_stance";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private Stances() {
    }

    static void press(ServerPlayer player) {
        boolean active = ACTIVE.contains(player.getUUID());
        if (player.isShiftKeyDown()) {
            StanceElement element = element(player).next();
            saveElement(player, element);
            apply(player, active);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    active ? "stance.mdvlcraft.switched_active" : "stance.mdvlcraft.switched", element.displayName(), element.effect())));
        } else {
            apply(player, !active);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    active ? "stance.mdvlcraft.off" : "stance.mdvlcraft.on", element(player).displayName(), element(player).effect())));
        }
    }

    /** Stance ability lost (its skill locked or its tab erased): switch it off. */
    static void deactivate(ServerPlayer player) {
        if (ACTIVE.contains(player.getUUID())) {
            apply(player, false);
        }
    }

    public static StanceElement element(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return data.contains(ELEMENT_KEY) ? StanceElement.values()[data.getByte(ELEMENT_KEY)] : StanceElement.FIRE;
    }

    private static void saveElement(ServerPlayer player, StanceElement element) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putByte(ELEMENT_KEY, (byte) element.ordinal());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Puts the modifier of the current element on (when active) and every other stance modifier off, then syncs. */
    private static void apply(ServerPlayer player, boolean active) {
        StanceElement current = element(player);
        for (StanceElement element : StanceElement.values()) {
            AttributeInstance instance = player.getAttribute(element.attribute.get());
            if (instance == null) {
                throw new IllegalStateException("Player has no " + element.attribute.get().getDescriptionId() + " attribute");
            }
            instance.removeModifier(element.modifier.getId());
            if (active && element == current) {
                instance.addTransientModifier(element.modifier);
            }
        }
        if (active) {
            ACTIVE.add(player.getUUID());
        } else {
            ACTIVE.remove(player.getUUID());
        }
        BinderNetwork.sendToTrackingAndSelf(player, packet(player));
    }

    private static StanceSyncPacket packet(ServerPlayer player) {
        return new StanceSyncPacket(player.getId(), element(player), ACTIVE.contains(player.getUUID()));
    }

    /** The wheel shows the player's current element from the start. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        BinderNetwork.sendTo(player, packet(player));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    /** A respawned player is a new entity without the old transient modifiers. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ACTIVE.remove(player.getUUID());
        BinderNetwork.sendToTrackingAndSelf(player, packet(player));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target) {
            BinderNetwork.sendTo((ServerPlayer) event.getEntity(), packet(target));
        }
    }
}
