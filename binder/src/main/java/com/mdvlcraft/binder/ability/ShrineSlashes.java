package com.mdvlcraft.binder.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A binder player's Malevolent Shrine only cuts while its owner holds the cast key on Dismantle or Cleave
 * (mixin/cursedfate/SureHitEffectInDomainAreaProcedureMixin for the closed domain, ShrineOnTickProcedureMixin for the
 * open shrine). Domains of Cursed Fate's own bosses have no player owner and always cut.
 */
public final class ShrineSlashes {
    private ShrineSlashes() {
    }

    /** The player owning a domain, from the UUID Cursed Fate stored under {@code key}; null if it is not a player. */
    @Nullable
    public static ServerPlayer owner(Entity domain, String key) {
        String uuid = domain.getPersistentData().getString(key);
        if (uuid.isEmpty()) {
            return null;
        }
        return domain.level().getPlayerByUUID(UUID.fromString(uuid)) instanceof ServerPlayer player ? player : null;
    }

    /** True while the owner's Shrine must hold its slashes: they are not holding Dismantle or Cleave. */
    public static boolean held(ServerPlayer owner) {
        return !TechniqueRunner.isSlashing(owner);
    }
}
