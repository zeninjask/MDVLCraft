package com.mdvlcraft.binder.chat;

import com.fen.mmorpgtitles.TitleData;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.ChatLinePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Player chat reads "Name - Title: message" ("Name: message" without a class title, skills/ClassTitles) and goes to every
 * player as a ChatLinePacket, whose client types the message out with the text blip (client/TypedChat). Hovering the
 * title shows its archetypes. The server console logs the line as usual.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class PlayerChat {
    private PlayerChat() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Component prefix = prefix(player);
        event.setCanceled(true);
        player.server.sendSystemMessage(Component.empty().append(prefix).append(event.getMessage()));
        BinderNetwork.sendToAll(new ChatLinePacket(prefix, event.getMessage()));
    }

    private static Component prefix(ServerPlayer player) {
        MutableComponent prefix = Component.empty().append(player.getDisplayName());
        String title = TitleData.getDisplayed(player);
        if (!title.isBlank()) {
            String archetypes = TitleData.getDescription(player, title);
            prefix.append(" - ").append(Component.literal(title).withStyle(style ->
                archetypes.isBlank() ? style : style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(archetypes)))));
        }
        return prefix.append(": ");
    }
}
