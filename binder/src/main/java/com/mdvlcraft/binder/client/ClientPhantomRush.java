package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.PhantomRush;
import com.mdvlcraft.binder.network.PhantomRushPacket;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MultiTargetEntityCastData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** Assassination Rush on the client: picked targets show as Iron's multi-target recast (icon and orbs at the top of the
 * screen, targeting marks on the picked creatures); a rush assassination's animations play at double speed. */
public final class ClientPhantomRush {
    private ClientPhantomRush() {
    }

    public static void update(PhantomRushPacket packet) {
        if (packet.fastTarget() < 0) {
            PhantomRush.CLIENT_FAST.remove(packet.player());
        } else {
            PhantomRush.CLIENT_FAST.put(packet.player(), packet.fastTarget());
        }

        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.getId() != packet.player()) {
            return;
        }

        if (packet.picked().isEmpty()) {
            ClientMagicData.getRecasts().removeRecast(PhantomRush.ICON_SPELL);
            return;
        }

        MultiTargetEntityCastData targets = new MultiTargetEntityCastData(new LivingEntity[0]);
        packet.picked().forEach(targets::addTarget);
        int picked = packet.picked().size();
        ClientMagicData.getRecasts().forceAddRecast(new RecastInstance(PhantomRush.ICON_SPELL, 1, packet.total(), 400, CastSource.NONE, targets) {
            {
                this.remainingRecasts = picked;
            }
        });
    }
}
