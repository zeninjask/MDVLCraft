package com.mdvlcraft.binder.superchef;

import com.p1nero.invincible.client.InvincibleKeyMappings;
import com.p1nero.invincible.skill.ComboBasicAttack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** The Super Chef combo (Invincible's key combo skill); its tooltip lists the combos with the player's own key names. */
public class ChefComboAttack extends ComboBasicAttack {
    public ChefComboAttack(ComboBasicAttack.Builder builder) {
        super(builder);
    }

    @Override
    public List<Component> getTooltipOnItem(ItemStack stack, CapabilityItem capability, PlayerPatch<?> playerpatch) {
        Component k1 = InvincibleKeyMappings.getTranslatableKey1();
        Component k2 = InvincibleKeyMappings.getTranslatableKey2();
        Component k3 = InvincibleKeyMappings.getTranslatableKey3();
        Object[][] args = {{k1, k2, k3}, {k2, k2, k2, k2, k1, k2}, {k1, k2, k3}, {k1, k1, k3}, {k1, k2, k3}, {k2, k2, k3}, {k2, k1, k3}, {k1, k2, k3}};
        List<Component> list = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            list.add(Component.translatable("item.mdvlcraft.super_chef.tip" + (i + 1), args[i]).withStyle(ChatFormatting.GRAY));
        }
        return list;
    }
}
