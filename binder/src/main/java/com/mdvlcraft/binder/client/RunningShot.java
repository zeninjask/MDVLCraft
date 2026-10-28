package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.specs.Specs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Running Shot (specs): drawing a bow while sprinting doesn't slow the player. This event fires just before
 * LocalPlayer.aiStep multiplies the input by 0.2 for item use, so the input is scaled up by 5 to cancel it out.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class RunningShot {
    private RunningShot() {
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.isUsingItem() && !player.isPassenger() && player.isSprinting() && player.getUseItem().getItem() instanceof BowItem
            && Specs.has(player, "running_shot")) {
            event.getInput().leftImpulse *= 5.0F;
            event.getInput().forwardImpulse *= 5.0F;
        }
    }
}
