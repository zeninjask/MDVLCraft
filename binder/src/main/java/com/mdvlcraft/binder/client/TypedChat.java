package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.mixin.ChatComponentAccessor;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Chat lines typed out like Dragon Quest IX dialogue: the speaker shows at once, the message appears at 60 letters a
 * second, and while any message is still appearing the text blip (sounds/chat/blip.ogg, a 663 Hz tone of 50 ms) plays
 * every 67.5 ms, as DQ IX does. The blip plays on the Voice/Speech volume, everywhere, not from the speaker.
 * mixin/ChatComponentMixin draws each chat line through visible(); lines rebuilt by a chat resize show in full.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class TypedChat {
    private static final double LETTERS_PER_SECOND = 60.0;
    private static final long BLIP_GAP_NANOS = 67_500_000L;
    private static final ResourceLocation BLIP = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "chat.blip");
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<GuiMessage.Line, Typing> TYPING = new WeakHashMap<>();
    private static long typingEnds;
    private static long nextBlip;

    /** start: when the message began; from: the line's first letter, counted from the message's first letter. */
    private record Typing(long start, int from) {
    }

    private TypedChat() {
    }

    public static void show(Component prefix, Component message) {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor)chat).mdvlcraft$trimmedMessages();
        GuiMessage.Line newestBefore = lines.isEmpty() ? null : lines.get(0);
        chat.addMessage(Component.empty().append(prefix).append(message));

        long now = System.nanoTime();
        int added = 0;
        while (added < lines.size() && lines.get(added) != newestBefore) {
            added++;
        }
        int from = -prefix.getString().length();
        for (int i = added - 1; i >= 0; i--) {
            GuiMessage.Line line = lines.get(i);
            TYPING.put(line, new Typing(now, from));
            from += length(line.content());
        }

        if (now >= typingEnds) {
            nextBlip = now;
        }
        typingEnds = Math.max(typingEnds, now + (long)(message.getString().length() / LETTERS_PER_SECOND * 1e9));
    }

    /** The part of a chat line typed out so far. */
    public static FormattedCharSequence visible(GuiMessage.Line line) {
        Typing typing = TYPING.get(line);
        if (typing == null) {
            return line.content();
        }
        int shown = (int)((System.nanoTime() - typing.start) / 1e9 * LETTERS_PER_SECOND) - typing.from;
        if (shown >= length(line.content())) {
            TYPING.remove(line);
            return line.content();
        }
        return sink -> {
            int[] count = {0};
            return line.content().accept((index, style, codePoint) -> count[0]++ < shown && sink.accept(index, style, codePoint));
        };
    }

    private static int length(FormattedCharSequence text) {
        int[] count = {0};
        text.accept((index, style, codePoint) -> {
            count[0]++;
            return true;
        });
        return count[0];
    }

    @SubscribeEvent
    public static void onRenderTick(RenderTickEvent event) {
        long now = System.nanoTime();
        if (event.phase != Phase.START || now >= typingEnds || now < nextBlip) {
            return;
        }
        nextBlip = Math.max(nextBlip + BLIP_GAP_NANOS, now);
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(BLIP, SoundSource.VOICE, 1.0F, 1.0F, RANDOM, false, 0,
            SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }
}
