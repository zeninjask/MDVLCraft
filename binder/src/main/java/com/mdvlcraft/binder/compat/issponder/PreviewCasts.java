package com.mdvlcraft.binder.compat.issponder;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;

public final class PreviewCasts {
    private static final ResourceLocation PREVIEW_DIMENSION = ResourceLocation.fromNamespaceAndPath("iss_ponder", "spell_preview");

    private PreviewCasts() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, SpellPreCastEvent.class, PreviewCasts::onPreCast);
    }

    private static void onPreCast(SpellPreCastEvent event) {
        if (event.isCanceled() && event.getEntity() instanceof FakePlayer caster && caster.level().dimension().location().equals(PREVIEW_DIMENSION)) {
            event.setCanceled(false);
        }
    }
}
