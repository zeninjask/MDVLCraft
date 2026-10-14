package com.mdvlcraft.binder.content;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The summoned demon swords can't be dropped: a tossed sword goes straight back into the inventory, and on death it
 * vanishes instead of dropping (cast the summon again for a new one).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class DemonSwordKeep {
    private DemonSwordKeep() {
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!DemonSwordArts.isDemonSword(event.getEntity().getItem())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCanceled(true);
        player.getInventory().add(event.getEntity().getItem());
        player.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) {
            event.getDrops().removeIf(drop -> DemonSwordArts.isDemonSword(drop.getItem()));
        }
    }
}
