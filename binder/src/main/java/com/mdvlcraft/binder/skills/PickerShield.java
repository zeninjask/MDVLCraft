package com.mdvlcraft.binder.skills;

import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Nothing can hurt a player while they pick their classes: while an Origins layer (origin, Martial class, Job) still
 * waits for a choice (its picker can't be closed), and while the vocation picker is open on a player with no vocation
 * (the client reports it, network/PickerOpenPacket). Damage that ignores invulnerability (the void, /kill) still hurts.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PickerShield {
    private static final Set<UUID> PICKER_OPEN = new HashSet<>();

    private PickerShield() {
    }

    public static void setOpen(ServerPlayer player, boolean open) {
        if (open) {
            PICKER_OPEN.add(player.getUUID());
        } else {
            PICKER_OPEN.remove(player.getUUID());
        }
    }

    private static boolean shielded(ServerPlayer player) {
        boolean originsPending = !IOriginContainer.get(player).map(IOriginContainer::hasAllOrigins).orElse(true);
        return originsPending || PICKER_OPEN.contains(player.getUUID()) && Respec.picked(player).isEmpty();
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && shielded(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        PICKER_OPEN.remove(event.getEntity().getUUID());
    }
}
