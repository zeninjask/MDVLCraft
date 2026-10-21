package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.ability.AbilityActions;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The balance passes of 2.0.28 to 2.0.30 changed the trees and costs, so each player's first login on 2.0.30 gives back
 * every point spent on their vocation tab and Origin tab (levels and archetypes stay) to spend again. Done once per player,
 * marked in their persisted data (a new flag per release that needs it); players who join for the first time are only marked.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BalanceRespec {
    private static final String DONE = "mdvlcraft_balance_respec_2_0_30";

    private BalanceRespec() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (kept.getBoolean(DONE)) {
            return;
        }

        boolean refunded = Respec.vocation(player).map(tab -> {
            tab.resetSkills(player);
            return true;
        }).orElse(false);
        refunded |= OriginTabs.tab(player)
            .flatMap(path -> SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", path)))
            .filter(tab -> tab.isUnlocked(player))
            .map(tab -> {
                tab.resetSkills(player);
                return true;
            }).orElse(false);
        if (refunded) {
            AbilityActions.dropUngranted(player);
            player.sendSystemMessage(Component.translatable("message.mdvlcraft.balance_respec").withStyle(ChatFormatting.GOLD));
        }

        kept.putBoolean(DONE, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
    }
}
