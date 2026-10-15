package com.mdvlcraft.binder.wukong;

import com.mdvlcraft.binder.skills.Respec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Only the Wukong vocation (Monk + Spirit) fights with the Ruyi Jingu Bang's moveset: for anyone else the staff's
 * Epic Fight capability reads as empty (no combo, no innate skill, no staff idle or run), so it swings like a plain item.
 * The server reads the picked archetypes from the skill trees, the client from what skills/ArchetypeLevels sent
 * (client/ClientArchetypes, set by setClientCheck), so other players animate the same on every screen.
 * Applied in Epic Fight's held-item capability and held-item change (mixin/epicfight/*HeldItemMixin, LivingEntityPatchHoldingMixin).
 */
public final class WukongGate {
    public static final List<String> ARCHETYPES = List.of("monk", "spirit");
    private static Predicate<Player> clientCheck = player -> false;

    private WukongGate() {
    }

    public static void setClientCheck(Predicate<Player> check) {
        clientCheck = check;
    }

    public static boolean isWukong(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return Respec.picked(serverPlayer).containsAll(ARCHETYPES);
        }
        return player.level().isClientSide && clientCheck.test(player);
    }

    /** The capability as the holder may use it: empty for a staff in the hands of anyone but the Wukong vocation. */
    public static CapabilityItem gate(LivingEntityPatch<?> holder, CapabilityItem capability) {
        if (capability != null && capability.getWeaponCategory() == WukongCategories.WK_STAFF
            && holder.getOriginal() instanceof Player player && !isWukong(player)) {
            return CapabilityItem.EMPTY;
        }
        return capability;
    }
}
