package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.monk.Fists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import yesman.epicfight.client.input.EpicFightKeyMappings;

public final class MonkGuardInput {
    private MonkGuardInput() {
    }

    public static boolean guardKeyForMonk(int inputKey) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && EpicFightKeyMappings.GUARD.getKey().getValue() == inputKey) {
            AttributeInstance unarmedParry = player.getAttribute(BinderAttributes.UNARMED_PARRY.get());
            return unarmedParry != null && unarmedParry.getValue() >= 1.0 && Fists.fisted(player);
        } else {
            return false;
        }
    }
}
