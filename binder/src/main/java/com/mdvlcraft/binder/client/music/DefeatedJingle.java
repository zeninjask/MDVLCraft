package com.mdvlcraft.binder.client.music;

import com.mdvlcraft.binder.music.Jingle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Defeated plays when the death screen opens. While PlayerRevive holds a downed player there is no death screen; it
 * opens once they give up or bleed out. You Died swaps in its own death screens, so only the first one counts.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class DefeatedJingle {
    private DefeatedJingle() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen && !(Minecraft.getInstance().screen instanceof DeathScreen)) {
            MusicDirector.playJingle(Jingle.DEFEATED);
        }
    }
}
