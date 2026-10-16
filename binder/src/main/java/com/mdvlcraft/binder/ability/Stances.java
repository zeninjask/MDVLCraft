package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.StanceSyncPacket;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "mdvlcraft")
public final class Stances {
    private static final String ELEMENT_KEY = "mdvlcraft_stance";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private Stances() {
    }

    static void press(ServerPlayer player) {
        boolean active = ACTIVE.contains(player.getUUID());
        if (player.isShiftKeyDown()) {
            StanceElement element = next(player, element(player));
            saveElement(player, element);
            apply(player, active);
            player.connection
                .send(
                    new ClientboundSetActionBarTextPacket(
                        Component.translatable(
                            active ? "stance.mdvlcraft.switched_active" : "stance.mdvlcraft.switched", element.displayName(), element.effect()
                        )
                    )
                );
        } else {
            apply(player, !active);
            player.connection
                .send(
                    new ClientboundSetActionBarTextPacket(
                        Component.translatable(active ? "stance.mdvlcraft.off" : "stance.mdvlcraft.on", element(player).displayName(), element(player).effect())
                    )
                );
        }
    }

    /** The element after `from` in the cycle that the player has: the four basic ones and every unlocked form. */
    private static StanceElement next(ServerPlayer player, StanceElement from) {
        StanceElement[] all = StanceElement.values();
        for (int i = 1; i <= all.length; i++) {
            StanceElement candidate = all[(from.ordinal() + i) % all.length];
            if (has(player, candidate)) {
                return candidate;
            }
        }
        return from;
    }

    public static boolean has(ServerPlayer player, StanceElement element) {
        return element.form == null
            || Ability.byId(element.formId()).map(ability -> AbilityGrants.level(player, ability) > 0).orElse(false);
    }

    /** Casting a form: switches into it and turns the stance on. */
    public static void enter(ServerPlayer player, StanceElement element) {
        saveElement(player, element);
        apply(player, true);
        player.connection.send(new ClientboundSetActionBarTextPacket(
            Component.translatable("stance.mdvlcraft.on", element.displayName(), element.effect())));
    }

    /** The active element of a player's stance, if the stance is on. */
    public static Optional<StanceElement> active(Player player) {
        return ACTIVE.contains(player.getUUID()) ? Optional.of(element(player)) : Optional.empty();
    }

    static void deactivate(ServerPlayer player) {
        if (ACTIVE.contains(player.getUUID())) {
            apply(player, false);
        }
    }

    public static StanceElement element(Player player) {
        CompoundTag data = player.getPersistentData().getCompound("PlayerPersisted");
        return data.contains("mdvlcraft_stance") ? StanceElement.values()[data.getByte("mdvlcraft_stance")] : StanceElement.FIRE;
    }

    private static void saveElement(ServerPlayer player, StanceElement element) {
        CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
        persisted.putByte("mdvlcraft_stance", (byte)element.ordinal());
        player.getPersistentData().put("PlayerPersisted", persisted);
    }

    private static void apply(ServerPlayer player, boolean active) {
        StanceElement current = element(player);

        for (StanceElement element : StanceElement.values()) {
            if (element.attribute == null) {
                continue;
            }
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

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        BinderNetwork.sendTo(player, packet(player));
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        ACTIVE.remove(player.getUUID());
        BinderNetwork.sendToTrackingAndSelf(player, packet(player));
    }

    @SubscribeEvent
    public static void onStartTracking(StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target) {
            BinderNetwork.sendTo((ServerPlayer)event.getEntity(), packet(target));
        }
    }
}
