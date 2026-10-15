package com.mdvlcraft.binder.wukong;

import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Only a player with the Monkey King identity (mdvlcraft:wukong, taught by the first node of the Wukong vocation's tree)
 * equipped fights with the Ruyi Jingu Bang's moveset: for anyone else the staff's Epic Fight capability reads as empty
 * (no combo, no innate skill, no staff idle or run), so it swings like a plain item. Epic Fight syncs every player's
 * equipped skills to the players tracking them, so other players animate the same on every screen.
 * Applied in Epic Fight's held-item capability and held-item change (mixin/epicfight/*HeldItemMixin, LivingEntityPatchHoldingMixin),
 * so equipping or removing the identity while holding the staff takes effect at the next item switch.
 */
public final class WukongGate {
    private WukongGate() {
    }

    /** The capability as the holder may use it: empty for a staff in the hands of anyone without the Monkey King identity. */
    public static CapabilityItem gate(LivingEntityPatch<?> holder, CapabilityItem capability) {
        if (capability != null && capability.getWeaponCategory() == WukongCategories.WK_STAFF
            && !(holder instanceof PlayerPatch<?> player && player.getSkill(SkillSlots.IDENTITY).getSkill() == WukongStaffSetup.IDENTITY)) {
            return CapabilityItem.EMPTY;
        }
        return capability;
    }
}
