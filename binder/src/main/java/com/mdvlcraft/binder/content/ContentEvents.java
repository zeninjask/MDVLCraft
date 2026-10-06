package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keeps forbidden items (see DisabledContent) out of trades, creative tabs and drops, keeps the spell-making blocks
 * unusable where structures placed them, and stops Iron's and Cursed Fate mobs spawning naturally (structures,
 * spawners, spells and commands still spawn them).
 */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class ContentEvents {
    private ContentEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && DisabledContent.isDisabled(event.getEntity().getType())) {
            event.setSpawnCancelled(true);
        }
    }

    /** Every merchant (villagers, the wandering trader, Iron's priests and wizards) drops forbidden offers before trading. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof Merchant merchant) {
            merchant.getOffers().removeIf(offer -> DisabledContent.isForbidden(offer.getResult())
                    || DisabledContent.isForbidden(offer.getBaseCostA())
                    || DisabledContent.isForbidden(offer.getCostB()));
        }
    }

    /** The scroll forge and inscription table placed in Iron's structures do nothing. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (DisabledContent.isForbidden(event.getLevel().getBlockState(event.getPos()).getBlock().asItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> forbidden = new ArrayList<>();
        for (Map.Entry<ItemStack, ?> entry : event.getEntries()) {
            if (DisabledContent.isForbidden(entry.getKey())) {
                forbidden.add(entry.getKey());
            }
        }
        forbidden.forEach(event.getEntries()::remove);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> DisabledContent.isForbidden(drop.getItem()));
    }
}
